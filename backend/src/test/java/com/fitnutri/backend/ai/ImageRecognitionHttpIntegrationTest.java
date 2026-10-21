package com.fitnutri.backend.ai;

import com.fasterxml.jackson.databind.*;
import com.fitnutri.backend.foodreference.*;
import java.math.BigDecimal;
import com.fitnutri.backend.support.TestDatabase;
import com.fitnutri.backend.user.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.net.*;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.*;
import static org.junit.jupiter.api.Assertions.*;

/** Real Spring session/CSRF + owned PostgreSQL + controlled HTTP provider, not live-model evidence. */
@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT,properties={"app.ai.key=synthetic-only-key","app.ai.image-model=synthetic-image","app.ai.image-enabled=true"})
@ActiveProfiles("integration-test")
class ImageRecognitionHttpIntegrationTest {
    private static final TestDatabase DB=TestDatabase.create();
    private static final ControlledAiProvider PROVIDER;
    static {try{PROVIDER=new ControlledAiProvider();}catch(Exception e){throw new ExceptionInInitializerError(e);}Runtime.getRuntime().addShutdownHook(new Thread(()->{PROVIDER.close();DB.close();}));}
    @DynamicPropertySource static void db(DynamicPropertyRegistry r){r.add("spring.datasource.url",DB::url);r.add("spring.datasource.username",DB::username);r.add("spring.datasource.password",DB::password);}
    @TestConfiguration static class ProviderTestConfig {@Bean @Primary AiTransport controlledTransport(){return PROVIDER.transport;}}
    @LocalServerPort int port;
    @Autowired FoodDatasetRepository datasets;@Autowired FoodReferenceRepository foods;@Autowired AuthService auth;@Autowired JdbcTemplate jdbc;
    final ObjectMapper json=new ObjectMapper();Client client;FoodReference reference;String proposedName;
    class Client {
        final HttpClient http=HttpClient.newBuilder().cookieHandler(new CookieManager(null,CookiePolicy.ACCEPT_ALL)).build();String csrf;
        Client() throws Exception {csrf=body(send("GET","/api/auth/csrf",null)).get("token").asText();}
        HttpResponse<String> send(String method,String path,Object data) throws Exception {var b=HttpRequest.newBuilder(URI.create("http://127.0.0.1:"+port+path));if(csrf!=null)b.header("X-CSRF-TOKEN",csrf);if(data!=null)b.header("Content-Type","application/json");return http.send(b.method(method,data==null?HttpRequest.BodyPublishers.noBody():HttpRequest.BodyPublishers.ofString(json.writeValueAsString(data))).build(),HttpResponse.BodyHandlers.ofString());}
        void login(String email) throws Exception {assertEquals(200,send("POST","/api/auth/login",Map.of("email",email,"password","Synthetic123")).statusCode());csrf=body(send("GET","/api/auth/csrf",null)).get("token").asText();}
        HttpResponse<String> upload(byte[] bytes,String mime) throws Exception {String boundary="Synthetic"+UUID.randomUUID();var data=new ByteArrayOutputStream();data.write(("--"+boundary+"\r\nContent-Disposition: form-data; name=\"image\"; filename=\"synthetic-upload\"\r\nContent-Type: "+mime+"\r\n\r\n").getBytes(StandardCharsets.UTF_8));data.write(bytes);data.write(("\r\n--"+boundary+"--\r\n").getBytes(StandardCharsets.UTF_8));var b=HttpRequest.newBuilder(URI.create("http://127.0.0.1:"+port+"/api/food/recognize")).header("Content-Type","multipart/form-data; boundary="+boundary);if(csrf!=null)b.header("X-CSRF-TOKEN",csrf);return http.send(b.POST(HttpRequest.BodyPublishers.ofByteArray(data.toByteArray())).build(),HttpResponse.BodyHandlers.ofString());}
    }
    @BeforeEach void arrange() throws Exception {
        PROVIDER.status=200;PROVIDER.body=ControlledAiProvider.envelope("{\"items\":[]}");client=new Client();String email="image-"+UUID.randomUUID()+"@example.test";
        assertEquals(201,client.send("POST","/api/auth/register",Map.of("email",email,"password","Synthetic123","displayName","Synthetic image test")).statusCode());client.login(email);
        var setup=new LinkedHashMap<String,Object>();setup.put("expectedUserVersion",0);setup.put("expectedProfileVersion",null);setup.put("displayName","Synthetic image test");setup.put("birthYear",1990);setup.put("adultConfirmed",true);setup.put("sex","PREFER_NOT_TO_SAY");setup.put("heightCm",170);setup.put("activityLevel","LIGHT");setup.put("goalType",null);setup.put("dailyTargetKj",null);setup.put("energyUnit","KJ");setup.put("healthSafetyAnswer","NO");setup.put("qaConsentGranted",false);setup.put("zone","Australia/Sydney");setup.put("initialWeight",Map.of("recordDate","2020-01-01","weightKg",70));assertEquals(200,client.send("POST","/api/profile/onboarding",setup).statusCode());
        proposedName="Synthetic rice "+UUID.randomUUID();reference=newReference(proposedName,true,true,true);
    }
    FoodReference newReference(String name,boolean active,boolean verified,boolean complete) throws Exception {
        // Build immutable fixture content before its first INSERT; never disable catalogue protection.
        var dataset=new FoodDataset();dataset.setName("SYNTHETIC_IMAGE_FIXTURE_ONLY");dataset.setRelease("test");dataset.setRevision(UUID.randomUUID().toString());dataset.setSourceUrl("https://example.test/synthetic-image-food");dataset.setContentSha256(java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(("SYNTHETIC_SOURCE_ONLY:"+name+":"+complete+":"+active+":"+verified).getBytes(StandardCharsets.UTF_8))));dataset.setNutrientPolicy("CORE4_REQUIRED");dataset.setSourceNote("Synthetic matching fixture, not official food facts");dataset.setVerified(verified);datasets.saveAndFlush(dataset);
        var food=new FoodReference();food.setDataset(dataset);food.setPublicFoodKey("synthetic-"+UUID.randomUUID());food.setFoodName(name);food.setOfficialName(name);food.setDisplayName(name);food.setActive(active);food.setEnergyKj100g(new BigDecimal("659"));food.setProteinG100g(complete?new BigDecimal("3.1"):null);food.setFatG100g(new BigDecimal("0.2"));food.setCarbsG100g(new BigDecimal("34.7"));return foods.saveAndFlush(food);
    }
    JsonNode body(HttpResponse<String> r) throws Exception {return json.readTree(r.body());}
    byte[] png() throws Exception {var out=new ByteArrayOutputStream();assertTrue(ImageIO.write(new BufferedImage(2,2,BufferedImage.TYPE_INT_RGB),"png",out));return out.toByteArray();}
    Map<String,Integer> counts(){return Map.of("food",jdbc.queryForObject("SELECT count(*) FROM food_record",Integer.class),"exercise",jdbc.queryForObject("SELECT count(*) FROM exercise_record",Integer.class),"receipt",jdbc.queryForObject("SELECT count(*) FROM submission_receipt",Integer.class));}
    String candidate(String name){return ControlledAiProvider.envelope("{\"items\":[{\"name\":\""+name+"\",\"suggestedGrams\":150}]}");}

    @Test void actualHttpReturnsOnlyRealSelectableReferenceIdsAndNeverWritesRecords() throws Exception {
        // Arrange unselectable lookalikes to prove matcher uses canonical active/verified/core rules.
        newReference(proposedName,false,true,true);
        newReference(proposedName,true,false,true);
        newReference(proposedName,true,true,false);
        var before=counts();int calls=PROVIDER.calls.get();PROVIDER.body=candidate(proposedName);
        // Act through actual cookie/CSRF/controller/transport/read-only matcher.
        var response=client.upload(png(),"image/png");assertEquals(200,response.statusCode(),response.body());JsonNode value=body(response);
        // Assert canonical DTO/source/IDs and no record/receipt side effects.
        assertEquals(Set.of("candidates","notices","requestId"),json.convertValue(value,Map.class).keySet());assertEquals(response.headers().firstValue("X-Request-Id").orElseThrow(),value.get("requestId").asText());assertEquals("no-store",response.headers().firstValue("Cache-Control").orElseThrow());
        var row=value.get("candidates").get(0);UUID.fromString(row.get("candidateKey").asText());assertEquals(5,row.size());assertFalse(row.has("confidence"));assertFalse(row.has("nutrients"));assertFalse(row.has("mealType"));assertEquals(1,row.get("referenceMatches").size());var match=row.get("referenceMatches").get(0);assertEquals(reference.getId().toString(),match.get("referenceId").asText());assertTrue(match.get("dataset").get("verified").asBoolean());assertTrue(match.get("active").asBoolean());assertEquals(5,match.size());assertEquals(before,counts());assertEquals(calls+1,PROVIDER.calls.get());
    }
    @Test void noFoodAndUnknownFoodStayEmptyOrUnmatchedWithoutInventedReferences() throws Exception {var before=counts();var empty=client.upload(png(),"image/png");assertEquals(200,empty.statusCode());assertEquals(0,body(empty).get("candidates").size());assertFalse(body(empty).get("notices").isEmpty());PROVIDER.body=candidate("NoSuchSyntheticFoodZZZZ");var unknown=client.upload(png(),"image/png");assertEquals(200,unknown.statusCode());assertEquals(0,body(unknown).get("candidates").get(0).get("referenceMatches").size());assertEquals(before,counts());}
    @Test void realApiFileValidationAndProviderFailuresDoNotCreateAnyRecords() throws Exception {var before=counts();int calls=PROVIDER.calls.get();assertEquals(400,client.upload(new byte[]{1,2,3},"image/jpeg").statusCode());assertEquals(400,client.upload(new byte[8*1024*1024+1],"image/png").statusCode());assertEquals(calls,PROVIDER.calls.get());PROVIDER.body="private-provider-body synthetic-only-key";var bad=client.upload(png(),"image/png");assertEquals(502,bad.statusCode());assertEquals("PROVIDER_BAD_RESPONSE",body(bad).get("code").asText());assertFalse(bad.body().contains("private-provider-body"));assertFalse(bad.body().contains("synthetic-only-key"));PROVIDER.status=401;assertEquals(503,client.upload(png(),"image/png").statusCode());assertEquals(before,counts());}
    @Test void anonymousMissingCsrfAndAdministratorAreDeniedBeforeProvider() throws Exception {int calls=PROVIDER.calls.get();var anonymous=new Client();assertEquals(401,anonymous.upload(png(),"image/png").statusCode());String csrf=client.csrf;client.csrf=null;assertEquals(403,client.upload(png(),"image/png").statusCode());client.csrf=csrf;String adminEmail="image-admin-"+UUID.randomUUID()+"@example.test";auth.bootstrapAdmin(new RegisterRequest(adminEmail,"Synthetic123","Synthetic administrator"));var admin=new Client();admin.login(adminEmail);assertEquals(403,admin.upload(png(),"image/png").statusCode());assertEquals(calls,PROVIDER.calls.get());}
    @Test void existingRecordAndReceiptSurviveImageRecognitionUnchanged() throws Exception {var payload=new LinkedHashMap<String,Object>();payload.put("submissionId",UUID.randomUUID());payload.put("recordDate","2020-01-02");payload.put("zone","Australia/Sydney");payload.put("source","MANUAL");payload.put("items",List.of(Map.of("clientEntryId",UUID.randomUUID(),"referenceId",reference.getId().toString(),"quantityGrams",100,"mealType","LUNCH")));var created=client.send("POST","/api/records/food/batch",payload);assertEquals(201,created.statusCode(),created.body());String id=body(created).get("records").get(0).get("id").asText(),submission=payload.get("submissionId").toString();String recordBefore=client.send("GET","/api/records/food/"+id,null).body(),proofBefore=client.send("GET","/api/submissions/"+submission,null).body();var before=counts();PROVIDER.body=candidate(proposedName);assertEquals(200,client.upload(png(),"image/png").statusCode());assertEquals(recordBefore,client.send("GET","/api/records/food/"+id,null).body());assertEquals(proofBefore,client.send("GET","/api/submissions/"+submission,null).body());assertEquals(before,counts());}
    @Test void onlyExplicitReviewedImageBatchCreatesCanonicalSnapshotsAndOneReceipt() throws Exception {
        var before=counts();PROVIDER.body=candidate(proposedName);var recognized=client.upload(png(),"image/png");assertEquals(200,recognized.statusCode());var candidate=body(recognized).get("candidates").get(0);assertEquals(before,counts());
        // This explicit confirmation changes the suggested 150g to 200g and supplies the user's meal/date.
        String referenceId=candidate.get("referenceMatches").get(0).get("referenceId").asText();var payload=Map.of("submissionId",UUID.randomUUID(),"recordDate","2020-01-02","zone","Australia/Sydney","source","IMAGE","items",List.of(Map.of("clientEntryId",UUID.randomUUID(),"referenceId",referenceId,"quantityGrams",200,"mealType","DINNER")));
        var saved=client.send("POST","/api/records/food/batch",payload);assertEquals(201,saved.statusCode(),saved.body());String id=body(saved).get("records").get(0).get("id").asText();var record=body(client.send("GET","/api/records/food/"+id,null));assertEquals("IMAGE",record.get("source").asText());assertEquals("DINNER",record.get("mealType").asText());assertEquals("2020-01-02",record.get("recordDate").asText());assertEquals(200,record.get("quantityGrams").asInt());assertEquals(0,new BigDecimal("1318.000").compareTo(record.get("nutrients").get("energyKj").decimalValue()));
        assertEquals(before.get("food")+1,counts().get("food"));assertEquals(before.get("receipt")+1,counts().get("receipt"));assertEquals(before.get("exercise"),counts().get("exercise"));assertEquals(200,client.send("POST","/api/records/food/batch",payload).statusCode());assertEquals(before.get("food")+1,counts().get("food"));assertEquals(before.get("receipt")+1,counts().get("receipt"));
    }

}
