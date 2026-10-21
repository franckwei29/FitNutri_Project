package com.fitnutri.backend.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import java.net.*;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/** Explicit synthetic provider, bound only to loopback. Never a real model success fixture. */
public final class ControlledAiProvider implements AutoCloseable {
    private final HttpServer server;private final HttpClient http;
    public final AtomicInteger calls=new AtomicInteger();
    public volatile String body=envelope("{\"items\":[]}");public volatile int status=200;
    public volatile String lastRequest;public final AiTransport transport;public volatile Runnable beforeReply=()->{};
    public ControlledAiProvider() throws Exception {this(1000);}
    public ControlledAiProvider(long processingTimeoutMillis) throws Exception {
        server=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);
        server.createContext("/v1/",exchange->{calls.incrementAndGet();lastRequest=new String(exchange.getRequestBody().readAllBytes(),StandardCharsets.UTF_8);try{beforeReply.run();byte[] bytes=body.getBytes(StandardCharsets.UTF_8);exchange.getResponseHeaders().set("Content-Type","application/json");exchange.sendResponseHeaders(status,bytes.length);try(var output=exchange.getResponseBody()){output.write(bytes);}finally{exchange.close();}}catch(java.io.IOException ignored){/* expected timeout/cancellation in controlled tests */}finally{exchange.close();}});server.start();
        http=HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NEVER).build();
        transport=new AiTransport(http,new AiProviderConfig(1000,processingTimeoutMillis),new AiFeatureConfiguration("synthetic-only-key","synthetic-image","synthetic-audio","synthetic-parse","synthetic-qa",true,true,true,true),URI.create("http://127.0.0.1:"+server.getAddress().getPort()+"/v1/"));
    }
    public static String envelope(String structured){try{return new ObjectMapper().writeValueAsString(Map.of("status","completed","output",List.of(Map.of("type","message","role","assistant","status","completed","content",List.of(Map.of("type","output_text","text",structured))))));}catch(Exception e){throw new IllegalStateException("Could not create synthetic provider fixture.");}}
    @Override public void close(){server.stop(0);http.close();}
}
