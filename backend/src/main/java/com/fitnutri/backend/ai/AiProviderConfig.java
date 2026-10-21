package com.fitnutri.backend.ai;

import java.net.http.HttpClient;
import java.net.URI;
import org.springframework.beans.factory.annotation.Autowired;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Bounded server-only transport settings. Constructing the app never probes a paid endpoint. */
@Configuration(proxyBeanMethods=false)
public class AiProviderConfig {
    private final Duration connectTimeout,processingTimeout;
    private final URI baseUri;
    public AiProviderConfig(long connectMs,long processingMs){this(connectMs,processingMs,"https://api.openai.com/v1/");}
    @Autowired
    public AiProviderConfig(@Value("${app.ai.connect-timeout-ms:10000}")long connectMs,
                            @Value("${app.ai.processing-timeout-ms:60000}")long processingMs,
                            @Value("${app.ai.base-url:https://api.openai.com/v1/}")String baseUrl) {
        if(connectMs<100||connectMs>30000||processingMs<100||processingMs>180000)
            throw new IllegalArgumentException("AI transport timeouts are outside the supported bounds.");
        URI uri;
        try{uri=URI.create(baseUrl);}catch(Exception ignored){throw new IllegalArgumentException("The AI provider URL is invalid.");}
        if(!"https".equals(uri.getScheme())||uri.getHost()==null||uri.getUserInfo()!=null||uri.getQuery()!=null||uri.getFragment()!=null||!"/v1/".equals(uri.getPath())||!(uri.getPort()==-1||uri.getPort()==443))
            throw new IllegalArgumentException("The AI provider URL must use HTTPS and the reviewed API base path.");
        baseUri=uri;
        connectTimeout=Duration.ofMillis(connectMs);processingTimeout=Duration.ofMillis(processingMs);
    }
    public URI baseUri(){return baseUri;}
    public Duration processingTimeout(){return processingTimeout;}
    @Bean(name="aiProviderHttpClient",destroyMethod="close")
    public HttpClient httpClient(){return HttpClient.newBuilder().connectTimeout(connectTimeout).followRedirects(HttpClient.Redirect.NEVER).build();}
}
