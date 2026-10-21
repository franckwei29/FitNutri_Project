package com.fitnutri.backend.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fitnutri.backend.common.BusinessException;
import com.sun.net.httpserver.HttpServer;
import java.net.*;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import static org.junit.jupiter.api.Assertions.*;

/** Controlled local provider HTTP only. Never a real-model smoke or a database test. */
class AiTransportTest {
    private final ObjectMapper json=new ObjectMapper();
    private final AtomicInteger calls=new AtomicInteger();
    private HttpServer provider;private ExecutorService executor;private HttpClient http;
    private volatile int status=200;private volatile String response="{\"status\":\"completed\",\"output\":[]}";
    private volatile String responseType="application/json",requestType,authorization,path,retryAfter,location;
    private volatile byte[] requestBytes;private volatile long delayHeaders,delayBody;
    private AiTransport transport;
    @BeforeEach void arrange() throws Exception {
        provider=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);executor=Executors.newCachedThreadPool();provider.setExecutor(executor);
        provider.createContext("/v1/",exchange->{
            calls.incrementAndGet();path=exchange.getRequestURI().getPath();requestBytes=exchange.getRequestBody().readAllBytes();requestType=exchange.getRequestHeaders().getFirst("Content-Type");authorization=exchange.getRequestHeaders().getFirst("Authorization");
            try {if(delayHeaders>0)Thread.sleep(delayHeaders);byte[] bytes=response.getBytes(StandardCharsets.UTF_8);exchange.getResponseHeaders().set("Content-Type",responseType);exchange.getResponseHeaders().set("x-request-id","req_synthetic_01");if(retryAfter!=null)exchange.getResponseHeaders().set("Retry-After",retryAfter);if(location!=null)exchange.getResponseHeaders().set("Location",location);exchange.sendResponseHeaders(status,bytes.length);if(delayBody>0)Thread.sleep(delayBody);exchange.getResponseBody().write(bytes);}catch(InterruptedException e){Thread.currentThread().interrupt();}catch(java.io.IOException ignored){/* expected client cancellation */}finally{exchange.close();}
        });provider.start();http=HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(1)).followRedirects(HttpClient.Redirect.NEVER).build();
        transport=new AiTransport(http,new AiProviderConfig(1000,1000),features("synthetic-only-secret",true),URI.create("http://127.0.0.1:"+provider.getAddress().getPort()+"/v1/"));
    }
    @AfterEach void cleanup(){TransactionSynchronizationManager.setActualTransactionActive(false);provider.stop(0);executor.shutdownNow();http.close();}
    private AiFeatureConfiguration features(String key,boolean enabled){return new AiFeatureConfiguration(key,"synthetic-image","synthetic-audio","synthetic-parse","synthetic-qa",enabled,enabled,enabled,enabled);}
    private ObjectNode payload(){ObjectNode body=json.createObjectNode();body.put("input","Synthetic test input");body.put("max_output_tokens",512);var format=body.putObject("text").putObject("format");format.put("type","json_schema");format.put("name","synthetic_result");format.put("strict",true);var schema=format.putObject("schema");schema.put("type","object");schema.put("additionalProperties",false);schema.putObject("properties");schema.putArray("required");return body;}
    private void assertSafe(BusinessException e){assertFalse(e.toString().contains("synthetic-only-secret"));assertFalse(e.toString().contains("private-provider-body"));assertNull(e.getCause());}

    @Test void jsonUsesConfiguredModelStoreFalseAndNoToolsOrRawResponseToString() throws Exception {
        // Arrange / Act: one actual request to the controlled provider, no retry.
        var result=transport.sendJson(AiFeature.IMAGE,payload());
        // Assert the wire contract, not a canned application response.
        var body=json.readTree(requestBytes);assertEquals("/v1/responses",path);assertEquals("Bearer synthetic-only-secret",authorization);assertEquals("application/json",requestType);
        assertEquals("synthetic-image",body.get("model").asText());assertFalse(body.get("store").asBoolean());assertFalse(body.get("stream").asBoolean());assertFalse(body.has("tools"));assertTrue(body.at("/text/format/strict").asBoolean());
        assertEquals("completed",result.json().get("status").asText());assertEquals("req_synthetic_01",result.requestId());assertFalse(result.toString().contains("output"));assertEquals(1,calls.get());
    }
    @Test void parsingAndQaUseTheirOwnEnvironmentModels() throws Exception {transport.sendJson(AiFeature.SPEECH_PARSING,payload());assertEquals("synthetic-parse",json.readTree(requestBytes).get("model").asText());transport.sendJson(AiFeature.QA,payload());assertEquals("synthetic-qa",json.readTree(requestBytes).get("model").asText());assertEquals(2,calls.get());}
    @ParameterizedTest @CsvSource({"401,503,FEATURE_NOT_CONFIGURED","403,503,FEATURE_NOT_CONFIGURED","429,429,RATE_LIMITED","500,502,PROVIDER_BAD_RESPONSE","503,502,PROVIDER_BAD_RESPONSE","400,502,PROVIDER_BAD_RESPONSE","302,502,PROVIDER_BAD_RESPONSE"})
    void providerErrorsNeverBecomeLocalSessionExpiryOrLeakProviderBody(int upstream,int local,String code){status=upstream;response="private-provider-body synthetic-only-secret";var error=assertThrows(BusinessException.class,()->transport.sendJson(AiFeature.IMAGE,payload()));assertEquals(local,error.status().value());assertEquals(code,error.code());assertEquals(1,calls.get());assertSafe(error);}
    @Test void emptyKeyDisabledFlagAndUnsafeConfigurationFailBeforeNetwork(){for(var config:List.of(features("",true),features("synthetic-only-secret",false),features("bad\r\nkey",true))){var client=new AiTransport(http,new AiProviderConfig(1000,1000),config,URI.create("http://127.0.0.1:"+provider.getAddress().getPort()+"/v1/"));var error=assertThrows(BusinessException.class,()->client.sendJson(AiFeature.IMAGE,payload()));assertEquals(503,error.status().value());assertSafe(error);}assertEquals(0,calls.get());}
    @Test void rejectsToolsClientModelAndMissingSchemaBeforeNetwork(){for(String field:List.of("tools","model","previous_response_id","conversation")){var body=payload();body.put(field,"untrusted");assertThrows(BusinessException.class,()->transport.sendJson(AiFeature.IMAGE,body));}var invalid=payload();invalid.remove("text");assertThrows(BusinessException.class,()->transport.sendJson(AiFeature.IMAGE,invalid));assertEquals(0,calls.get());}
    @Test void rejectsActiveDatabaseTransactionBeforeAnyProviderCall(){TransactionSynchronizationManager.setActualTransactionActive(true);var error=assertThrows(BusinessException.class,()->transport.sendJson(AiFeature.QA,payload()));assertEquals("AI_TRANSACTION_BOUNDARY",error.code());assertEquals(0,calls.get());}
    @Test void multipartUsesRealBinaryWithGeneratedFilenameModelAndJsonFormat(){byte[] audio={0,1,2,13,10,(byte)255};response="{\"text\":\"Synthetic transcript\"}";var result=transport.sendMultipart(AiFeature.TRANSCRIPTION,audio,"audio/webm");assertEquals("Synthetic transcript",result.json().get("text").asText());assertEquals("/v1/audio/transcriptions",path);assertTrue(requestType.startsWith("multipart/form-data; boundary="));String wire=new String(requestBytes,StandardCharsets.ISO_8859_1);assertTrue(wire.contains("name=\"model\"\r\n\r\nsynthetic-audio"));assertTrue(wire.contains("name=\"response_format\"\r\n\r\njson"));assertTrue(wire.contains("filename=\"audio.webm\""));assertTrue(wire.contains(new String(audio,StandardCharsets.ISO_8859_1)));assertEquals(1,calls.get());}
    @Test void multipartRejectsEmptyOversizeMimeInjectionAndWrongFeatureBeforeNetwork(){for(byte[] data:List.of(new byte[0],new byte[8*1024*1024+1]))assertThrows(BusinessException.class,()->transport.sendMultipart(AiFeature.TRANSCRIPTION,data,"audio/webm"));assertThrows(BusinessException.class,()->transport.sendMultipart(AiFeature.TRANSCRIPTION,new byte[]{1},"audio/webm\r\nInjected: true"));assertThrows(BusinessException.class,()->transport.sendMultipart(AiFeature.IMAGE,new byte[]{1},"audio/webm"));assertThrows(BusinessException.class,()->transport.sendMultipart(AiFeature.TRANSCRIPTION,new byte[]{1},null));assertEquals(0,calls.get());}
    @Test void malformedHtmlArrayAndOversizeResponseAreBoundedAndSanitized(){for(String value:List.of("private-provider-body", "[]", "{\"text\":\""+"x".repeat(512*1024)+"\"}")){response=value;var error=assertThrows(BusinessException.class,()->transport.sendJson(AiFeature.QA,payload()));assertEquals("PROVIDER_BAD_RESPONSE",error.code());assertSafe(error);}response="{}";responseType="text/html";assertThrows(BusinessException.class,()->transport.sendJson(AiFeature.QA,payload()));}
    @Test void timeoutIncludesDelayedResponseBodyNotOnlyHeaders(){delayBody=500;var client=new AiTransport(http,new AiProviderConfig(1000,100),features("synthetic-only-secret",true),URI.create("http://127.0.0.1:"+provider.getAddress().getPort()+"/v1/"));long start=System.nanoTime();var error=assertThrows(BusinessException.class,()->client.sendJson(AiFeature.IMAGE,payload()));assertEquals("PROVIDER_TIMEOUT",error.code());assertEquals(504,error.status().value());assertTrue(Duration.ofNanos(System.nanoTime()-start).toMillis()<1500);assertEquals(1,calls.get());assertSafe(error);}
    @Test void invalidTimeoutConfigurationIsRejectedWithoutPrintingValues(){assertThrows(IllegalArgumentException.class,()->new AiProviderConfig(0,1000));assertThrows(IllegalArgumentException.class,()->new AiProviderConfig(1000,180001));}

    @Test void rateLimitDelayIsBoundedNumericMetadataWithoutRawProviderHeader(){status=429;for(String supplied:List.of("12","99999999999999999999999","private-provider-body")){retryAfter=supplied;var error=assertThrows(com.fitnutri.backend.common.RateLimitedException.class,()->transport.sendJson(AiFeature.QA,payload()));assertEquals(supplied.equals("12")?12:30,error.retryAfterSeconds());assertSafe(error);}}
    @Test void actualRedirectDoesNotFollowOrSendCredentialsToAnotherPath(){status=302;location="http://127.0.0.1:"+provider.getAddress().getPort()+"/v1/untrusted-target";assertThrows(BusinessException.class,()->transport.sendJson(AiFeature.QA,payload()));assertEquals(1,calls.get());assertEquals("/v1/responses",path);}
    @Test void productionClientHasConnectTimeoutAndNeverFollowsRedirects(){try(var configured=new AiProviderConfig(10000,60000).httpClient()){assertEquals(Duration.ofSeconds(10),configured.connectTimeout().orElseThrow());assertEquals(HttpClient.Redirect.NEVER,configured.followRedirects());}assertEquals(0,calls.get());}
    @Test void timeoutBeforeHeadersAlsoMapsToSafe504(){delayHeaders=500;var client=new AiTransport(http,new AiProviderConfig(1000,100),features("synthetic-only-secret",true),URI.create("http://127.0.0.1:"+provider.getAddress().getPort()+"/v1/"));var error=assertThrows(BusinessException.class,()->client.sendJson(AiFeature.IMAGE,payload()));assertEquals("PROVIDER_TIMEOUT",error.code());assertSafe(error);}

    @Test void providerUrlIsServerConfiguredHttpsWithoutCredentialsQueryOrFragment(){
        assertEquals(URI.create("https://api.openai.com/v1/"),new AiProviderConfig(1000,1000).baseUri());
        assertEquals(URI.create("https://provider.example.test/v1/"),new AiProviderConfig(1000,1000,"https://provider.example.test/v1/").baseUri());
        for(String value:List.of("http://provider.example.test/v1/","https://secret@provider.example.test/v1/","https://provider.example.test/v1/?token=secret","https://provider.example.test/v1/#private","https://provider.example.test/v1/../")){
            var error=assertThrows(IllegalArgumentException.class,()->new AiProviderConfig(1000,1000,value));assertFalse(error.toString().contains("secret"));assertNull(error.getCause());
        }
    }
}
