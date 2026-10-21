package com.fitnutri.backend.speech;

import com.fasterxml.jackson.databind.*;
import com.fitnutri.backend.ai.*;
import com.fitnutri.backend.support.TestDatabase;
import com.fitnutri.backend.user.*;
import java.io.*;
import java.net.*;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.*;
import static org.junit.jupiter.api.Assertions.*;

/** Actual Session/CSRF + owned PostgreSQL + actual codecs + controlled provider, never live speech evidence. */
@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT,properties={"app.ai.key=synthetic-only-key","app.ai.transcription-model=synthetic-audio","app.ai.transcription-enabled=true"})
@ActiveProfiles("integration-test")
class SpeechTranscriptionHttpIntegrationTest {
    static final TestDatabase DB=TestDatabase.create();static final ControlledAiProvider PROVIDER;
    static {try{PROVIDER=new ControlledAiProvider();}catch(Exception e){throw new ExceptionInInitializerError(e);}Runtime.getRuntime().addShutdownHook(new Thread(()->{PROVIDER.close();DB.close();}));}
    @DynamicPropertySource static void db(DynamicPropertyRegistry r){r.add("spring.datasource.url",DB::url);r.add("spring.datasource.username",DB::username);r.add("spring.datasource.password",DB::password);}
    @TestConfiguration static class ProviderConfig {@Bean @Primary AiTransport controlledTransport(){return PROVIDER.transport;}}
    @LocalServerPort int port;@Autowired AuthService auth;@Autowired JdbcTemplate jdbc;
    final ObjectMapper json=new ObjectMapper();Client client;String email;String userId;
    class Client {
        final HttpClient http=HttpClient.newBuilder().cookieHandler(new CookieManager(null,CookiePolicy.ACCEPT_ALL)).build();String csrf;
        Client()throws Exception{csrf=body(send("GET","/api/auth/csrf",null)).get("token").asText();}
        HttpResponse<String> send(String method,String path,Object value)throws Exception{var b=HttpRequest.newBuilder(URI.create("http://127.0.0.1:"+port+path));if(csrf!=null)b.header("X-CSRF-TOKEN",csrf);if(value!=null)b.header("Content-Type","application/json");return http.send(b.method(method,value==null?HttpRequest.BodyPublishers.noBody():HttpRequest.BodyPublishers.ofString(json.writeValueAsString(value))).build(),HttpResponse.BodyHandlers.ofString());}
        void login(String email)throws Exception{assertEquals(200,send("POST","/api/auth/login",Map.of("email",email,"password","Synthetic123")).statusCode());csrf=body(send("GET","/api/auth/csrf",null)).get("token").asText();}
        HttpResponse<String> upload(byte[] bytes,String mime)throws Exception{return upload(bytes,mime,false);}
        HttpResponse<String> upload(byte[] bytes,String mime,boolean inject)throws Exception{String boundary="Synthetic"+UUID.randomUUID();var data=new ByteArrayOutputStream();data.write(("--"+boundary+"\r\nContent-Disposition: form-data; name=\"audio\"; filename=\"private-synthetic-original\"\r\nContent-Type: "+mime+"\r\n\r\n").getBytes(StandardCharsets.UTF_8));data.write(bytes);if(inject)data.write(("\r\n--"+boundary+"\r\nContent-Disposition: form-data; name=\"userId\"\r\n\r\n999999").getBytes(StandardCharsets.UTF_8));data.write(("\r\n--"+boundary+"--\r\n").getBytes(StandardCharsets.UTF_8));var b=HttpRequest.newBuilder(URI.create("http://127.0.0.1:"+port+"/api/speech/transcribe")).header("Content-Type","multipart/form-data; boundary="+boundary);if(csrf!=null)b.header("X-CSRF-TOKEN",csrf);return http.send(b.POST(HttpRequest.BodyPublishers.ofByteArray(data.toByteArray())).build(),HttpResponse.BodyHandlers.ofString());}
    }
    JsonNode body(HttpResponse<String> response)throws Exception{return json.readTree(response.body());}
    byte[] fixture(String name)throws Exception{try(var input=getClass().getResourceAsStream("/fixtures/ai/"+name)){assertNotNull(input);return input.readAllBytes();}}
    Map<String,Integer> counts(){return Map.of("food",jdbc.queryForObject("SELECT count(*) FROM food_record",Integer.class),"exercise",jdbc.queryForObject("SELECT count(*) FROM exercise_record",Integer.class),"receipt",jdbc.queryForObject("SELECT count(*) FROM submission_receipt",Integer.class));}
    @BeforeEach void arrange()throws Exception{
        PROVIDER.status=200;PROVIDER.body="{\"text\":\"Synthetic transcript from controlled provider only.\"}";client=new Client();email="speech-"+UUID.randomUUID()+"@example.test";var registered=client.send("POST","/api/auth/register",Map.of("email",email,"password","Synthetic123","displayName","Synthetic speech test"));assertEquals(201,registered.statusCode());userId=body(registered).get("id").asText();client.login(email);
        var setup=new LinkedHashMap<String,Object>();setup.put("expectedUserVersion",0);setup.put("expectedProfileVersion",null);setup.put("displayName","Synthetic speech test");setup.put("birthYear",1990);setup.put("adultConfirmed",true);setup.put("sex","PREFER_NOT_TO_SAY");setup.put("heightCm",170);setup.put("activityLevel","LIGHT");setup.put("goalType",null);setup.put("dailyTargetKj",null);setup.put("energyUnit","KJ");setup.put("healthSafetyAnswer","NO");setup.put("qaConsentGranted",false);setup.put("zone","Australia/Sydney");setup.put("initialWeight",Map.of("recordDate","2020-01-01","weightKg",70));assertEquals(200,client.send("POST","/api/profile/onboarding",setup).statusCode());
    }
    @Test void realWebmAndMp4ReturnCanonicalEditableTextWithoutRecordWrites()throws Exception{
        var before=counts();int calls=PROVIDER.calls.get();for(String name:List.of("synthetic-tone.webm","synthetic-tone.mp4")){var response=client.upload(fixture(name),name.endsWith("webm")?"audio/webm;codecs=opus":"audio/mp4");assertEquals(200,response.statusCode(),response.body());var value=body(response);assertEquals(3,value.size());assertEquals("Synthetic transcript from controlled provider only.",value.get("transcript").asText());assertFalse(value.get("notices").isEmpty());UUID.fromString(value.get("requestId").asText());assertEquals(response.headers().firstValue("X-Request-Id").orElseThrow(),value.get("requestId").asText());assertEquals("no-store",response.headers().firstValue("Cache-Control").orElseThrow());assertTrue(PROVIDER.lastRequest.contains("filename=\"audio.wav\""));assertFalse(PROVIDER.lastRequest.contains("private-synthetic-original"));assertFalse(PROVIDER.lastRequest.contains("SYNTHETIC_PRIVATE_METADATA"));}assertEquals(calls+2,PROVIDER.calls.get());assertEquals(before,counts());var capabilities=body(client.send("GET","/api/capabilities",null));assertTrue(capabilities.at("/features/transcription/available").asBoolean());assertFalse(capabilities.at("/features/qa/available").asBoolean());
    }
    @Test void emptyBadOversizedAndLongAudioAreRejectedBeforeProvider()throws Exception{var before=counts();int calls=PROVIDER.calls.get();assertEquals(400,client.upload(fixture("synthetic-tone.webm"),"audio/webm",true).statusCode());assertEquals(400,client.upload(new byte[0],"audio/webm").statusCode());assertEquals(400,client.upload(new byte[]{1,2,3},"audio/mp4").statusCode());assertEquals(400,client.upload(fixture("synthetic-tone.mp4"),"audio/webm").statusCode());assertEquals(400,client.upload(new byte[8*1024*1024+1],"audio/webm").statusCode());assertEquals(400,client.upload(fixture("synthetic-long-tone.webm"),"audio/webm").statusCode());assertEquals(413,client.upload(new byte[11*1024*1024],"audio/webm").statusCode());assertEquals(calls,PROVIDER.calls.get());assertEquals(before,counts());}
    @Test void anonymousCsrfAndAdminAreRejectedBeforeProvider()throws Exception{int calls=PROVIDER.calls.get();assertEquals(401,new Client().upload(fixture("synthetic-tone.webm"),"audio/webm").statusCode());String csrf=client.csrf;client.csrf=null;assertEquals(403,client.upload(fixture("synthetic-tone.webm"),"audio/webm").statusCode());client.csrf=csrf;String adminEmail="speech-admin-"+UUID.randomUUID()+"@example.test";auth.bootstrapAdmin(new RegisterRequest(adminEmail,"Synthetic123","Synthetic administrator"));var admin=new Client();admin.login(adminEmail);assertEquals(403,admin.upload(fixture("synthetic-tone.webm"),"audio/webm").statusCode());assertEquals(calls,PROVIDER.calls.get());}
    @Test void disabledAccountCannotBypassFreshSecurityGateWithItsExistingSession()throws Exception{String adminEmail="speech-admin-"+UUID.randomUUID()+"@example.test";auth.bootstrapAdmin(new RegisterRequest(adminEmail,"Synthetic123","Synthetic administrator"));var admin=new Client();admin.login(adminEmail);assertEquals(200,admin.send("PATCH","/api/admin/users/"+userId+"/deactivate",Map.of("expectedVersion",0)).statusCode());int calls=PROVIDER.calls.get();assertEquals(401,client.upload(fixture("synthetic-tone.webm"),"audio/webm").statusCode());assertEquals(calls,PROVIDER.calls.get());}
    @Test void invalidProviderAndUpstreamAuthAndRateErrorsAreSafeAndNeverCreateRecords()throws Exception{var before=counts();PROVIDER.body="private-provider-body synthetic-only-key";var response=client.upload(fixture("synthetic-tone.webm"),"audio/webm");assertEquals(502,response.statusCode());assertEquals("PROVIDER_BAD_RESPONSE",body(response).get("code").asText());assertFalse(response.body().contains("private-provider-body"));assertFalse(response.body().contains("synthetic-only-key"));PROVIDER.status=401;assertEquals(503,client.upload(fixture("synthetic-tone.webm"),"audio/webm").statusCode());PROVIDER.status=429;response=client.upload(fixture("synthetic-tone.webm"),"audio/webm");assertEquals(429,response.statusCode());assertEquals("30",response.headers().firstValue("Retry-After").orElseThrow());assertEquals(before,counts());}
}
