package com.fitnutri.backend.records.food;
import com.fitnutri.backend.foodreference.*;
import com.fitnutri.backend.profile.*;
import com.fitnutri.backend.records.*;
import com.fitnutri.backend.support.*;
import com.fitnutri.backend.user.*;
import java.math.BigDecimal;
import java.net.*;
import java.net.http.*;
import java.time.LocalDate;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.*;
import tools.jackson.databind.ObjectMapper;
import static org.junit.jupiter.api.Assertions.*;
/** Actual cookie/CSRF/controller/transaction/PostgreSQL journey; no frontend/browser or official-provider claim. */
@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT) @ActiveProfiles("integration-test")
class FoodRootHttpV2IT {
 static final TestDatabase DB=TestDatabase.create();static{Runtime.getRuntime().addShutdownHook(new Thread(DB::close));}
 @DynamicPropertySource static void database(DynamicPropertyRegistry r){r.add("spring.datasource.url",DB::url);r.add("spring.datasource.username",DB::username);r.add("spring.datasource.password",DB::password);}
 @LocalServerPort int port;@Autowired ObjectMapper json;@Autowired UserRepository users;@Autowired FoodDatasetRepository datasets;@Autowired FoodReferenceRepository references;@Autowired JdbcTemplate jdbc;
 HttpClient client=client();
 static HttpClient client(){return HttpClient.newBuilder().cookieHandler(new CookieManager(null,CookiePolicy.ACCEPT_ALL)).build();}
 HttpResponse<String> get(String path)throws Exception{return client.send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:"+port+path)).GET().build(),HttpResponse.BodyHandlers.ofString());}
 String csrf()throws Exception{var response=get("/api/auth/csrf");assertEquals(200,response.statusCode());return json.readTree(response.body()).get("token").asText();}
 HttpResponse<String> post(String path,Object body)throws Exception{return client.send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:"+port+path)).header("Content-Type","application/json").header("X-CSRF-TOKEN",csrf()).POST(HttpRequest.BodyPublishers.ofString(json.writeValueAsString(body))).build(),HttpResponse.BodyHandlers.ofString());}
 String registerAndLogin()throws Exception{String email="http-v2-"+UUID.randomUUID()+"@example.test";assertEquals(201,post("/api/auth/register",Map.of("email",email,"password","Synthetic123","displayName","Synthetic HTTP owner")).statusCode());assertEquals(200,post("/api/auth/login",Map.of("email",email,"password","Synthetic123")).statusCode());return email;}
 @Test void realSessionCanPreviewSaveReplayReadCurrentAndLegacyButAnotherOwnerCannot()throws Exception{
  String email=registerAndLogin();var owner=users.findByCanonicalEmail(email).orElseThrow();var date=LocalDate.of(2020,1,2);var setup=new CompleteOnboardingRequest(owner.getVersion(),null,"Synthetic HTTP owner",1990,true,Sex.PREFER_NOT_TO_SAY,new BigDecimal("170"),ActivityLevel.LIGHT,null,null,EnergyUnit.KJ,HealthSafetyAnswer.NO,false,"Australia/Sydney",new CompleteOnboardingRequest.InitialWeight(date,new BigDecimal("70")));var onboard=post("/api/profile/onboarding",setup);assertEquals(200,onboard.statusCode(),onboard.body());
  var reference=FoodV2TestFixtures.reference(datasets,references,"500");var entry=new FoodEntryInputs.Single(UUID.randomUUID(),reference.getId().toString(),new BigDecimal("100"),MealType.LUNCH,null,null);var input=new FoodEntryInputs.PreviewRequest(date,"Australia/Sydney",RecordSource.MANUAL,List.of(entry));var preview=post("/api/v2/food/preview",input);assertEquals(200,preview.statusCode(),preview.body());var batch=new FoodEntryInputs.BatchRequest(UUID.randomUUID(),date,input.zone(),input.source(),input.entries(),json.readTree(preview.body()).get("fingerprint").asText());var saved=post("/api/v2/food/batches",batch);assertEquals(201,saved.statusCode(),saved.body());var proof=json.readTree(saved.body());assertEquals(2,proof.get("schemaVersion").asInt());String id=proof.get("roots").get(0).get("id").asText();assertEquals(200,post("/api/v2/food/batches",batch).statusCode());
  var detail=get("/api/v2/food/entries/single/"+id);assertEquals(200,detail.statusCode(),detail.body());assertTrue(detail.headers().firstValue("cache-control").orElse("").contains("no-store"));var current=json.readTree(detail.body());assertEquals("CURRENT",current.get("dataQuality").asText());assertEquals(reference.getDisplayName(),current.get("displayName").asText());assertEquals(reference.getDisplayName(),current.get("componentDetails").get(0).get("name").asText());assertEquals(current.get("input").get("clientEntryId"),current.get("componentDetails").get(0).get("clientEntryId"));assertTrue(current.get("input").get("referenceId").isString());assertEquals(500,current.get("preview").get("totals").get("energyKj").asInt());var listed=get("/api/v2/food/entries?from=2020-01-02&to=2020-01-02&size=1");assertEquals(200,listed.statusCode());assertEquals(current,json.readTree(listed.body()).get("results").get(0));
  long legacy=jdbc.queryForObject("INSERT INTO food_record(user_id,created_at,recorded_at,recorded_date,client_entry_id,source,food_name,quantity_grams,meal_type) VALUES(?,now(),now(),?,gen_random_uuid(),'PHOTO','Synthetic retained HTTP legacy',123.456,'SNACK') RETURNING id",Long.class,owner.getId(),date);var legacyResponse=get("/api/v2/food/entries/single/"+legacy);assertEquals(200,legacyResponse.statusCode());var old=json.readTree(legacyResponse.body());assertEquals("LEGACY_UNVERIFIED",old.get("dataQuality").asText());assertEquals("Synthetic retained HTTP legacy",old.get("componentDetails").get(0).get("name").asText());assertTrue(old.get("componentDetails").get(0).get("referenceId").isNull());assertTrue(old.get("input").get("referenceId").isNull());assertTrue(old.get("preview").get("totals").get("energyKj").isNull());assertEquals("PHOTO",jdbc.queryForObject("SELECT source FROM food_record WHERE id=?",String.class,legacy));
  client=client();assertEquals(401,get("/api/v2/food/entries/single/"+id).statusCode());registerAndLogin();assertEquals(404,get("/api/v2/food/entries/single/"+id+"?userId="+owner.getId()).statusCode());assertEquals(404,get("/api/v2/food/entries/single/"+legacy).statusCode());assertTrue(json.readTree(get("/api/v2/food/entries?from=2020-01-02&to=2020-01-02").body()).get("results").isEmpty());
 }
}
