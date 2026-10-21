package com.fitnutri.backend.ai;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.StreamReadConstraints;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fitnutri.backend.common.BusinessException;
import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.net.http.*;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/** Shared provider wire boundary only. Adapters validate business schemas, consent and candidates. */
@Component
public final class AiTransport {
    private static final int MAX_AUDIO_BYTES=8*1024*1024,MAX_JSON_BYTES=12*1024*1024,MAX_RESPONSE_BYTES=512*1024;
    private static final Set<String> JSON_FIELDS=Set.of("input","instructions","text","max_output_tokens");
    private static final Map<String,String> AUDIO_TYPES=Map.of("audio/webm","webm","video/webm","webm","audio/mp4","mp4","video/mp4","mp4","audio/mpeg","mp3","audio/wav","wav","audio/x-wav","wav","audio/ogg","ogg","audio/flac","flac");
    private final HttpClient client;private final AiProviderConfig settings;private final AiFeatureConfiguration features;private final URI base;
    private final ObjectMapper json=new ObjectMapper(JsonFactory.builder().streamReadConstraints(StreamReadConstraints.builder().maxNestingDepth(32).maxStringLength(128*1024).maxNumberLength(64).build()).build());
    @Autowired
    public AiTransport(@Qualifier("aiProviderHttpClient")HttpClient client,AiProviderConfig settings,AiFeatureConfiguration features){this(client,settings,features,settings.baseUri());}
    /** Package-private loopback seam for controlled tests; production has no arbitrary upstream override. */
    AiTransport(HttpClient client,AiProviderConfig settings,AiFeatureConfiguration features,URI base){
        if(!settings.baseUri().equals(base)&&!("http".equals(base.getScheme())&&"127.0.0.1".equals(base.getHost())&&base.getPort()>0&&"/v1/".equals(base.getPath())&&base.getUserInfo()==null&&base.getQuery()==null&&base.getFragment()==null))throw new IllegalArgumentException("Unsupported AI provider transport endpoint.");
        if(client.followRedirects()!=HttpClient.Redirect.NEVER)throw new IllegalArgumentException("AI provider redirects must remain disabled.");
        this.client=client;this.settings=settings;this.features=features;this.base=base;
    }
    public void validateProviderConfiguration(AiFeature feature){
        if(TransactionSynchronizationManager.isActualTransactionActive())throw new BusinessException(HttpStatus.INTERNAL_SERVER_ERROR,"AI_TRANSACTION_BOUNDARY","The AI request must run outside a database transaction.");
        if(feature==null||!features.enabled(feature)||!features.configured(feature))throw AiExceptionMapper.unavailable();
    }
    public Response sendJson(AiFeature feature,ObjectNode payload){
        validateProviderConfiguration(feature);
        if(feature==AiFeature.TRANSCRIPTION||payload==null)throw AiExceptionMapper.invalidRequest();
        for(var names=payload.fieldNames();names.hasNext();)if(!JSON_FIELDS.contains(names.next()))throw AiExceptionMapper.invalidRequest();
        JsonNode input=payload.get("input"),format=payload.at("/text/format"),tokens=payload.get("max_output_tokens");
        if(input==null||!(input.isTextual()||input.isArray())||tokens==null||!tokens.isInt()||tokens.intValue()<64||tokens.intValue()>4096
            ||!"json_schema".equals(format.path("type").asText())||!format.path("strict").isBoolean()||!format.path("strict").asBoolean()
            ||!format.path("name").asText().matches("[A-Za-z0-9_-]{1,64}")||!format.path("schema").isObject()
            ||!"object".equals(format.at("/schema/type").asText())||!format.at("/schema/additionalProperties").isBoolean()||format.at("/schema/additionalProperties").asBoolean())throw AiExceptionMapper.invalidRequest();
        ObjectNode authoritative=payload.deepCopy();authoritative.put("model",features.model(feature));authoritative.put("store",false);authoritative.put("stream",false);
        byte[] body;try{body=json.writeValueAsBytes(authoritative);}catch(Exception ignored){throw AiExceptionMapper.invalidRequest();}
        if(body.length>MAX_JSON_BYTES)throw AiExceptionMapper.invalidRequest();
        return send("responses","application/json",HttpRequest.BodyPublishers.ofByteArray(body));
    }
    public Response sendMultipart(AiFeature feature,byte[] media,String contentType){
        validateProviderConfiguration(feature);
        if(feature!=AiFeature.TRANSCRIPTION)throw AiExceptionMapper.invalidRequest();
        String extension=contentType==null?null:AUDIO_TYPES.get(contentType);
        if(media==null||media.length==0||media.length>MAX_AUDIO_BYTES||extension==null)throw BusinessException.invalid("audio","Provide a supported audio file no larger than 8 MiB.");
        String boundary="FitNutri"+UUID.randomUUID().toString().replace("-","");
        String prefix=part(boundary,"model",features.model(feature))+part(boundary,"response_format","json")+"--"+boundary+"\r\nContent-Disposition: form-data; name=\"file\"; filename=\"audio."+extension+"\"\r\nContent-Type: "+contentType+"\r\n\r\n";
        return send("audio/transcriptions","multipart/form-data; boundary="+boundary,HttpRequest.BodyPublishers.ofByteArrays(List.of(prefix.getBytes(StandardCharsets.UTF_8),media.clone(),("\r\n--"+boundary+"--\r\n").getBytes(StandardCharsets.UTF_8))));
    }
    private static String part(String boundary,String name,String value){return "--"+boundary+"\r\nContent-Disposition: form-data; name=\""+name+"\"\r\n\r\n"+value+"\r\n";}
    private Response send(String endpoint,String contentType,HttpRequest.BodyPublisher body){
        var request=HttpRequest.newBuilder(base.resolve(endpoint)).header("Authorization","Bearer "+features.apiKey()).header("Content-Type",contentType).header("Accept","application/json").timeout(settings.processingTimeout()).POST(body).build();
        CompletableFuture<HttpResponse<byte[]>> pending=null;
        try{
            pending=client.sendAsync(request,info->new BoundedBody());
            // This deadline includes the full response body, unlike an InputStream read after headers.
            var received=pending.get(settings.processingTimeout().toMillis(),TimeUnit.MILLISECONDS);
            if(received.statusCode()<200||received.statusCode()>=300)throw AiExceptionMapper.upstream(received.statusCode(),received.headers().firstValue("Retry-After").orElse(null));
            String type=received.headers().firstValue("Content-Type").orElse("").split(";",2)[0].trim();
            if(!"application/json".equalsIgnoreCase(type))throw AiExceptionMapper.badResponse();
            JsonNode parsed;try{parsed=json.readTree(received.body());}catch(Exception ignored){throw AiExceptionMapper.badResponse();}
            if(parsed==null||!parsed.isObject())throw AiExceptionMapper.badResponse();
            String requestId=received.headers().firstValue("x-request-id").filter(value->value.matches("[A-Za-z0-9_-]{1,128}")).orElse(null);
            return new Response(parsed,requestId);
        }catch(TimeoutException e){if(pending!=null)pending.cancel(true);throw AiExceptionMapper.timeout();}
        catch(InterruptedException e){if(pending!=null)pending.cancel(true);Thread.currentThread().interrupt();throw AiExceptionMapper.badResponse();}
        catch(ExecutionException e){if(e.getCause() instanceof HttpTimeoutException)throw AiExceptionMapper.timeout();throw AiExceptionMapper.badResponse();}
    }
    public static final class Response {
        private final JsonNode json;private final String requestId;
        private Response(JsonNode json,String requestId){this.json=json;this.requestId=requestId;}
        public JsonNode json(){return json;}
        public String requestId(){return requestId;}
        @Override public String toString(){return "AiProviderResponse[requestId="+requestId+"]";}
    }
    private static final class BoundedBody implements HttpResponse.BodySubscriber<byte[]> {
        private final CompletableFuture<byte[]> result=new CompletableFuture<>();private final ByteArrayOutputStream bytes=new ByteArrayOutputStream();private Flow.Subscription subscription;
        public CompletionStage<byte[]> getBody(){return result;}
        public void onSubscribe(Flow.Subscription value){subscription=value;value.request(Long.MAX_VALUE);}
        public void onNext(List<ByteBuffer> chunks){for(ByteBuffer buffer:chunks){if(buffer.remaining()>MAX_RESPONSE_BYTES-bytes.size()){subscription.cancel();result.completeExceptionally(new IllegalStateException("AI response exceeds the transport limit."));return;}byte[] chunk=new byte[buffer.remaining()];buffer.get(chunk);bytes.writeBytes(chunk);}}
        public void onError(Throwable ignored){result.completeExceptionally(new IllegalStateException("AI response could not be read."));}
        public void onComplete(){result.complete(bytes.toByteArray());}
    }
}
