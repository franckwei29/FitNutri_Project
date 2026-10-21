package com.fitnutri.backend.capabilities;
import com.fasterxml.jackson.databind.*;
import com.fitnutri.backend.support.TestDatabase;
import com.fitnutri.backend.user.*;
import java.net.*;
import java.net.http.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.*;
import static org.junit.jupiter.api.Assertions.*;
@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT) @ActiveProfiles("integration-test")
class CapabilitiesHttpIntegrationTest {
    private static final TestDatabase DB=TestDatabase.create();static{Runtime.getRuntime().addShutdownHook(new Thread(DB::close));}
    @DynamicPropertySource static void db(DynamicPropertyRegistry r){r.add("spring.datasource.url",DB::url);r.add("spring.datasource.username",DB::username);r.add("spring.datasource.password",DB::password);}
    @LocalServerPort int port;@Autowired UserRepository users;
    private final ObjectMapper json=new ObjectMapper();private HttpClient client;private String csrf,email;
    @BeforeEach void signIn() throws Exception {client=HttpClient.newBuilder().cookieHandler(new CookieManager(null,CookiePolicy.ACCEPT_ALL)).build();csrf=null;email="capabilities-"+UUID.randomUUID()+"@example.test";csrf=status(200,send("GET","/api/auth/csrf",null)).get("token").asText();status(201,send("POST","/api/auth/register",Map.of("email",email,"password","Synthetic123","displayName","Synthetic capabilities")));status(200,send("POST","/api/auth/login",Map.of("email",email,"password","Synthetic123")));csrf=status(200,send("GET","/api/auth/csrf",null)).get("token").asText();}
    private HttpResponse<String> send(String method,String path,Object body) throws Exception {var r=HttpRequest.newBuilder(URI.create("http://127.0.0.1:"+port+path));if(csrf!=null)r.header("X-CSRF-TOKEN",csrf);if(body!=null)r.header("Content-Type","application/json");return client.send(r.method(method,body==null?HttpRequest.BodyPublishers.noBody():HttpRequest.BodyPublishers.ofString(json.writeValueAsString(body))).build(),HttpResponse.BodyHandlers.ofString());}
    private JsonNode status(int expected,HttpResponse<String> response) throws Exception {assertEquals(expected,response.statusCode(),response.body());return json.readTree(response.body());}
    @Test void authenticatedReadReportsActualNoDirectoryNoProviderStateWithoutSecretsAndAnonymousIs401() throws Exception {
        var response=send("GET","/api/capabilities",null);var body=status(200,response);assertEquals("no-store",response.headers().firstValue("Cache-Control").orElseThrow());assertEquals("mvp-1",body.get("schemaVersion").asText());assertEquals("Australia/Sydney",body.get("defaultZone").asText());assertTrue(body.get("catalogueRevision").asText().matches("[a-f0-9]{64}"));for(var directory:body.get("catalogues")){assertFalse(directory.get("ready").asBoolean());assertFalse(directory.get("verified").asBoolean());assertFalse(directory.get("reason").isNull());}for(var feature:body.get("features")){assertFalse(feature.get("configured").asBoolean());assertFalse(feature.get("available").asBoolean());assertFalse(feature.get("reason").isNull());}assertFalse(body.has("userId"));assertFalse(response.body().contains("apiKey"));assertFalse(response.body().contains("OPENAI_"));client=HttpClient.newHttpClient();assertEquals("UNAUTHENTICATED",status(401,send("GET","/api/capabilities",null)).get("code").asText());
    }
    @Test void unconfiguredImageRouteIs503BeforeProviderRatherThanReturningCandidates() throws Exception {
        String boundary="fitnutri-"+UUID.randomUUID();String upload="--"+boundary+"\r\nContent-Disposition: form-data; name=\"image\"; filename=\"synthetic.jpg\"\r\nContent-Type: image/jpeg\r\n\r\nsynthetic-gate-only\r\n--"+boundary+"--\r\n";var request=HttpRequest.newBuilder(URI.create("http://127.0.0.1:"+port+"/api/food/recognize")).header("X-CSRF-TOKEN",csrf).header("Content-Type","multipart/form-data; boundary="+boundary).POST(HttpRequest.BodyPublishers.ofString(upload)).build();var response=status(503,client.send(request,HttpResponse.BodyHandlers.ofString()));assertEquals("FEATURE_NOT_CONFIGURED",response.get("code").asText());assertFalse(response.has("candidates"));
    }
    @Test void administratorCanReadAuthenticatedMetadataAndDisabledSessionIsRejected() throws Exception {var user=users.findByCanonicalEmail(email).orElseThrow();user.setRole(User.Role.ADMIN);user=users.saveAndFlush(user);var metadata=status(200,send("GET","/api/capabilities",null));assertFalse(metadata.get("features").get("qa").get("available").asBoolean());user.setEnabled(false);users.saveAndFlush(user);assertEquals("UNAUTHENTICATED",status(401,send("GET","/api/capabilities",null)).get("code").asText());}
}
