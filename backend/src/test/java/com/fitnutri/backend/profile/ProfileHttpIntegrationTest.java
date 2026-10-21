package com.fitnutri.backend.profile;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fitnutri.backend.support.TestDatabase;
import com.fitnutri.backend.user.UserRepository;
import java.net.*;
import java.net.http.*;
import java.sql.Connection;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import static org.junit.jupiter.api.Assertions.*;

/** Real cookies, CSRF, MVC validation, transactions and PostgreSQL; every account is synthetic. */
@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("integration-test")
class ProfileHttpIntegrationTest {
    private static final TestDatabase DB=TestDatabase.create();
    static {Runtime.getRuntime().addShutdownHook(new Thread(DB::close));}
    @DynamicPropertySource static void db(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url",DB::url);r.add("spring.datasource.username",DB::username);r.add("spring.datasource.password",DB::password);
    }
    @LocalServerPort private int port;
    @Autowired private UserRepository users;
    @Autowired private ProfileService service;
    @Autowired private com.fitnutri.backend.submission.SubmissionService submissions;
    @Autowired private com.fitnutri.backend.submission.CanonicalSubmissionHasher submissionHasher;
    @Autowired private com.fitnutri.backend.records.FoodRecordRepository foodRecords;
    private final ObjectMapper json=new ObjectMapper();
    private HttpClient client;
    private String token,email;
    @BeforeEach void arrangeAccount() throws Exception {
        client=HttpClient.newBuilder().cookieHandler(new CookieManager(null,CookiePolicy.ACCEPT_ALL)).build();
        email="profile-"+UUID.randomUUID()+"@example.test";
        token=json.readTree(send("GET","/api/auth/csrf",null).body()).get("token").asText();
        assertStatus(201,send("POST","/api/auth/register",Map.of("email",email,"password","Synthetic123","displayName","Before")));
        assertStatus(200,send("POST","/api/auth/login",Map.of("email",email,"password","Synthetic123")));
        token=json.readTree(send("GET","/api/auth/csrf",null).body()).get("token").asText();
    }
    private HttpResponse<String> send(String method,String path,Object body) throws Exception {
        var request=HttpRequest.newBuilder(URI.create("http://127.0.0.1:"+port+path));
        if(token!=null) request.header("X-CSRF-TOKEN",token);
        if(body!=null) request.header("Content-Type","application/json");
        return client.send(request.method(method,body==null?HttpRequest.BodyPublishers.noBody():HttpRequest.BodyPublishers.ofString(json.writeValueAsString(body))).build(),HttpResponse.BodyHandlers.ofString());
    }
    private JsonNode assertStatus(int status,HttpResponse<String> response) throws Exception {
        assertEquals(status,response.statusCode(),response.body());return response.body().isBlank()?null:json.readTree(response.body());
    }
    private Map<String,Object> snapshot(JsonNode profile) {
        Map<String,Object> body=new LinkedHashMap<>();
        body.put("expectedUserVersion",profile.get("user").get("version").asLong());
        body.put("expectedProfileVersion",profile.get("profileVersion").isNull()?null:profile.get("profileVersion").asLong());
        body.put("displayName","After");body.put("birthYear",1990);body.put("adultConfirmed",true);
        body.put("sex","PREFER_NOT_TO_SAY");body.put("heightCm",170);body.put("activityLevel","LIGHT");
        body.put("goalType","MAINTAIN");body.put("dailyTargetKj",8400);body.put("energyUnit","KJ");
        body.put("healthSafetyAnswer","NO");body.put("qaConsentGranted",false);body.put("zone","Australia/Sydney");return body;
    }
    private JsonNode profile() throws Exception {return assertStatus(200,send("GET","/api/profile",null));}
    private Map<String,Object> onboarding() throws Exception {
        var body=snapshot(profile());body.put("initialWeight",Map.of("recordDate","2020-01-01","weightKg",70));return body;
    }
    private long countWeights() throws Exception {
        try(Connection c=DB.connect();var s=c.prepareStatement("SELECT count(*) FROM weight_entry WHERE user_id=?")) {
            s.setLong(1,users.findByCanonicalEmail(email).orElseThrow().getId());try(var rs=s.executeQuery()){rs.next();return rs.getLong(1);}
        }
    }
    @Test void missingProfileHasNullVersionAndNoInventedAnswers() throws Exception {
        var p=profile();assertTrue(p.get("profileVersion").isNull());assertTrue(p.get("currentWeight").isNull());
        assertFalse(p.get("adultConfirmed").asBoolean());assertFalse(p.get("qaConsentGranted").asBoolean());
        assertFalse(p.get("qaEligibility").get("eligible").asBoolean());assertFalse(p.toString().contains("password"));
        assertTrue(p.get("user").get("id").isTextual());assertTrue(p.get("qaConsentUpdatedAt").isNull());
    }
    @Test void falseConsentStillCompletesAtomicallyAndDuplicatePostCannotOverwrite() throws Exception {
        var body=onboarding();var saved=assertStatus(200,send("POST","/api/profile/onboarding",body));
        assertTrue(saved.get("onboardingCompleted").asBoolean());assertTrue(saved.get("adultConfirmed").asBoolean());
        assertFalse(saved.get("qaEligibility").get("eligible").asBoolean());assertEquals("After",saved.get("user").get("displayName").asText());
        assertEquals(70,saved.get("currentWeight").get("weightKg").asInt());assertEquals(1,countWeights());
        assertFalse(saved.get("qaConsentUpdatedAt").isNull());assertEquals(saved,profile());
        body.put("displayName","Must not overwrite");
        assertEquals("ONBOARDING_ALREADY_COMPLETED",assertStatus(409,send("POST","/api/profile/onboarding",body)).get("code").asText());
        assertEquals(saved,profile());assertEquals(1,countWeights());
    }
    @Test void explicitNullClearsGoalsAndSettingsNeverAppendWeight() throws Exception {
        var saved=assertStatus(200,send("POST","/api/profile/onboarding",onboarding()));
        var body=snapshot(saved);body.put("displayName","Changed");body.put("goalType",null);body.put("dailyTargetKj",null);
        var updated=assertStatus(200,send("PUT","/api/profile",body));
        assertTrue(updated.get("goalType").isNull());assertTrue(updated.get("dailyTargetKj").isNull());assertEquals(1,countWeights());
        assertEquals("Changed",updated.get("user").get("displayName").asText());
        assertTrue(updated.get("profileVersion").asLong()>saved.get("profileVersion").asLong());
        assertEquals(saved.get("qaConsentUpdatedAt"),updated.get("qaConsentUpdatedAt"));
        assertEquals(saved.get("healthSafetyUpdatedAt"),updated.get("healthSafetyUpdatedAt"));
    }
    @Test void omissionIsNotNullAndUnknownLegacyWeightCannotBeSilentlyAccepted() throws Exception {
        var saved=assertStatus(200,send("POST","/api/profile/onboarding",onboarding()));
        for(String field:List.of("goalType","dailyTargetKj","expectedProfileVersion","qaConsentGranted","adultConfirmed")) {
            var body=snapshot(saved);body.remove(field);assertStatus(400,send("PUT","/api/profile",body));
        }
        var body=snapshot(saved);body.put("weightKg",71);assertStatus(400,send("PUT","/api/profile",body));
        assertEquals(saved,profile());assertEquals(1,countWeights());
    }
    @Test void underageUnconfirmedInvalidZoneAndFutureWeightLeaveNoPartialData() throws Exception {
        for(var change:List.of(Map.entry("birthYear",(Object)2090),Map.entry("adultConfirmed",(Object)false),Map.entry("zone",(Object)"invalid-zone"),Map.entry("initialWeight",(Object)Map.of("recordDate","2090-01-01","weightKg",70)))) {
            var body=onboarding();body.put(change.getKey(),change.getValue());assertStatus(400,send("POST","/api/profile/onboarding",body));
            assertTrue(profile().get("profileVersion").isNull());assertEquals("Before",profile().get("user").get("displayName").asText());assertEquals(0,countWeights());
        }
    }
    @Test void staleUserOrProfileVersionRejectsWholeSnapshot() throws Exception {
        var saved=assertStatus(200,send("POST","/api/profile/onboarding",onboarding()));
        for(String field:List.of("expectedUserVersion","expectedProfileVersion")) {
            var body=snapshot(saved);body.put(field,999);body.put("displayName","Stale");
            assertEquals("VERSION_CONFLICT",assertStatus(409,send("PUT","/api/profile",body)).get("code").asText());assertEquals(saved,profile());
        }
    }
    @Test void databaseFailureAfterProfileAndUserFlushRollsBackAllWrites() throws Exception {
        long id=users.findByCanonicalEmail(email).orElseThrow().getId();
        // A pre-existing measurement creates a real late unique-constraint failure, in this owned DB only.
        try(Connection c=DB.connect();var s=c.prepareStatement("INSERT INTO weight_entry(user_id,weight_kg,entry_date,created_at) VALUES (?,65,'2020-01-01',CURRENT_TIMESTAMP)")) {s.setLong(1,id);s.executeUpdate();}
        assertStatus(409,send("POST","/api/profile/onboarding",onboarding()));
        var p=profile();assertTrue(p.get("profileVersion").isNull());assertFalse(p.get("onboardingCompleted").asBoolean());
        assertEquals("Before",p.get("user").get("displayName").asText());assertEquals(0,p.get("user").get("version").asLong());
        assertEquals(65,p.get("currentWeight").get("weightKg").asInt());assertEquals(1,countWeights());
    }
    @Test void legacyCompletedAdultReconfirmationUsesPutWithoutNewInitialWeightOrInventedConsentTime() throws Exception {
        var saved=assertStatus(200,send("POST","/api/profile/onboarding",onboarding()));
        try(Connection c=DB.connect();var s=c.prepareStatement("UPDATE user_profile SET adult_confirmed=false,qa_consent_updated_at=NULL,health_safety_updated_at=NULL WHERE user_id=?")) {
            s.setLong(1,Long.parseLong(saved.get("user").get("id").asText()));s.executeUpdate();
        }
        var legacy=profile();var updated=assertStatus(200,send("PUT","/api/profile",snapshot(legacy)));
        assertTrue(updated.get("adultConfirmed").asBoolean());assertTrue(updated.get("onboardingCompleted").asBoolean());assertEquals(1,countWeights());
        assertTrue(updated.get("qaConsentUpdatedAt").isNull());assertTrue(updated.get("healthSafetyUpdatedAt").isNull());
    }
    @Test void eligibilityUsesCurrentAnswerAndConsentInsteadOfStoredLegacyFlag() throws Exception {
        var saved=assertStatus(200,send("POST","/api/profile/onboarding",onboarding()));
        var body=snapshot(saved);body.put("qaConsentGranted",true);var eligible=assertStatus(200,send("PUT","/api/profile",body));
        assertTrue(eligible.get("qaEligibility").get("eligible").asBoolean());
        body=snapshot(eligible);body.put("qaConsentGranted",true);body.put("healthSafetyAnswer","YES_OR_UNSURE");
        var blocked=assertStatus(200,send("PUT","/api/profile",body));assertFalse(blocked.get("qaEligibility").get("eligible").asBoolean());
        assertTrue(blocked.get("onboardingCompleted").asBoolean());assertEquals(1,countWeights());
    }
    @Test void compatibilityGoalDeletionRequiresVersionsAndUsesSameSettingsService() throws Exception {
        var saved=assertStatus(200,send("POST","/api/profile/onboarding",onboarding()));
        assertStatus(400,send("DELETE","/api/profile/goal",null));
        var cleared=assertStatus(200,send("DELETE","/api/profile/goal?expectedUserVersion="+saved.get("user").get("version")+"&expectedProfileVersion="+saved.get("profileVersion"),null));
        assertTrue(cleared.get("goalType").isNull());assertEquals(saved.get("dailyTargetKj"),cleared.get("dailyTargetKj"));assertEquals(1,countWeights());
        assertStatus(409,send("DELETE","/api/profile/energy-goal?expectedUserVersion=999&expectedProfileVersion=999",null));assertEquals(cleared,profile());
    }
    @Test void concurrentOnboardingHasOneWinnerAndOneInitialWeight() throws Exception {
        long id=users.findByCanonicalEmail(email).orElseThrow().getId();
        var request=new CompleteOnboardingRequest(0L,null,"After",1990,true,Sex.OTHER,new java.math.BigDecimal("170"),ActivityLevel.LIGHT,null,null,EnergyUnit.KJ,HealthSafetyAnswer.NO,false,"Australia/Sydney",new CompleteOnboardingRequest.InitialWeight(java.time.LocalDate.of(2020,1,1),new java.math.BigDecimal("70")));
        var start=new java.util.concurrent.CountDownLatch(1);
        try(var pool=java.util.concurrent.Executors.newFixedThreadPool(2)) {
            java.util.concurrent.Callable<Integer> attempt=()->{start.await();try{service.completeOnboarding(id,request);return 200;}catch(com.fitnutri.backend.common.BusinessException e){assertEquals("ONBOARDING_ALREADY_COMPLETED",e.code());return e.status().value();}};
            var a=pool.submit(attempt);var b=pool.submit(attempt);start.countDown();
            var codes=new ArrayList<>(List.of(a.get(15,java.util.concurrent.TimeUnit.SECONDS),b.get(15,java.util.concurrent.TimeUnit.SECONDS)));Collections.sort(codes);
            assertEquals(List.of(200,409),codes);assertEquals(1,countWeights());assertTrue(profile().get("onboardingCompleted").asBoolean());
        }
    }

    @Test void weightHttpContractReturnsConflictItemAndExplicitVersions() throws Exception {
        var body=new LinkedHashMap<String,Object>();body.put("recordDate","2020-01-01");body.put("weightKg",70);body.put("expectedVersion",null);body.put("zone","Australia/Sydney");
        var created=assertStatus(201,send("POST","/api/profile/weight",body));assertTrue(created.get("weight").get("id").isTextual());assertTrue(created.get("profileVersion").isNull());
        assertEquals(created,assertStatus(200,send("POST","/api/profile/weight",body)));
        body.put("weightKg",71);var conflict=assertStatus(409,send("POST","/api/profile/weight",body));assertEquals("VERSION_CONFLICT",conflict.get("code").asText());assertEquals(created.get("weight"),conflict.get("currentItem"));
        body.put("expectedVersion",0);var corrected=assertStatus(200,send("POST","/api/profile/weight",body));assertEquals(1,corrected.get("weight").get("version").asInt());assertEquals(71,corrected.get("currentWeight").get("weightKg").asInt());
        assertStatus(409,send("POST","/api/profile/weight",body));assertEquals(1,countWeights());
        assertEquals(1,assertStatus(200,send("GET","/api/profile/weight?from=2020-01-01&to=2020-01-31",null)).size());
        assertStatus(400,send("GET","/api/profile/weight",null));
    }
    @Test void weightHttpRejectsMissingVersionLegacyDatesAndInvalidRange() throws Exception {
        assertStatus(400,send("POST","/api/profile/weight",Map.of("recordDate","2020-01-01","weightKg",70,"zone","Australia/Sydney")));
        assertStatus(400,send("POST","/api/profile/weight",Map.of("date","2020-01-01","weightKg",70)));
        assertStatus(400,send("GET","/api/profile/weight?from=2020-01-02&to=2020-01-01",null));
        assertStatus(400,send("GET","/api/profile/weight?from=2018-01-01&to=2020-01-01",null));assertEquals(0,countWeights());
    }

    @Test void authenticatedLegacyRecordWritesAndHardDeletesAreExplicitlyClosed() throws Exception {
        for(String kind:List.of("food","exercise")) {
            assertEquals("VALIDATION_ERROR",assertStatus(400,send("POST","/api/records/"+kind,Map.of("calories",999))).get("code").asText());
            assertStatus(400,send("PUT","/api/records/"+kind+"/1",Map.of("calories",999)));
            assertStatus(400,send("DELETE","/api/records/"+kind+"/1",null));
        }
    }

    @Test void realReceiptEndpointReturnsOnlyOwnerProofAndOtherSessionGets404() throws Exception {
        var submission=UUID.randomUUID();var clientId=UUID.randomUUID();var date=java.time.LocalDate.of(2020,1,1);var request=submissionHasher.food(submission,date,"Australia/Sydney",com.fitnutri.backend.records.RecordSource.MANUAL,List.of(new com.fitnutri.backend.submission.CanonicalSubmissionHasher.FoodItem(clientId,101L,java.math.BigDecimal.TEN,com.fitnutri.backend.records.MealType.LUNCH)));
        var owner=users.findByCanonicalEmail(email).orElseThrow();var proof=submissions.withSubmission(owner.getId(),request,user->{var row=new com.fitnutri.backend.records.FoodRecord();row.setUser(user);row.setFoodName("Synthetic receipt HTTP fixture");row.setQuantityGramsSnapshot(java.math.BigDecimal.TEN);row.setMealType(com.fitnutri.backend.records.MealType.LUNCH);row.setSource(com.fitnutri.backend.records.RecordSource.MANUAL);row.setRecordedAt(java.time.Instant.parse("2020-01-01T00:00:00Z"));row.setRecordDate(date);row.setClientEntryId(clientId);foodRecords.saveAndFlush(row);return List.of(new com.fitnutri.backend.submission.SubmissionReceiptResponse.RecordIdentity(row.getId().toString(),clientId,date,0));});
        var response=send("GET","/api/submissions/"+submission,null);var body=assertStatus(200,response);assertEquals("no-store",response.headers().firstValue("Cache-Control").orElseThrow());assertEquals(proof.records().getFirst().id(),body.get("records").get(0).get("id").asText());assertEquals(5,body.size());assertEquals(4,body.get("records").get(0).size());assertTrue(body.get("replayed").asBoolean());assertTrue(body.get("records").get(0).get("id").isTextual());assertFalse(body.has("userId"));assertFalse(body.has("requestSha256"));
        arrangeAccount();var other=assertStatus(404,send("GET","/api/submissions/"+submission,null));assertEquals("NOT_FOUND",other.get("code").asText());assertFalse(other.has("records"));
    }
    @Test void missingActivityCatalogueCannotExposeTheLegacyHardcodedMap() throws Exception {
        var unavailable=assertStatus(503,send("GET","/api/records/exercise/types",null));assertEquals("CATALOGUE_UNAVAILABLE",unavailable.get("code").asText());
    }
    @Test void missingVerifiedCatalogueIsExplicitlyUnavailableRatherThanSeededOrEmptySuccess() throws Exception {
        var unavailable=assertStatus(503,send("GET","/api/food-reference/search?query=rice",null));assertEquals("CATALOGUE_UNAVAILABLE",unavailable.get("code").asText());
    }


    @Test void concurrentHttpWeightCreatesUseOneDatedRowAndExposeTheWinningVersion() throws Exception {
        var firstBody=new LinkedHashMap<String,Object>();firstBody.put("recordDate","2020-01-03");firstBody.put("weightKg",70);firstBody.put("expectedVersion",null);firstBody.put("zone","Australia/Sydney");
        var secondBody=new LinkedHashMap<>(firstBody);secondBody.put("weightKg",71);
        var start=new java.util.concurrent.CountDownLatch(1);
        try(var pool=java.util.concurrent.Executors.newFixedThreadPool(2)) {
            var first=pool.submit(()->{start.await();return send("POST","/api/profile/weight",firstBody);});
            var second=pool.submit(()->{start.await();return send("POST","/api/profile/weight",secondBody);});start.countDown();
            var a=first.get(15,java.util.concurrent.TimeUnit.SECONDS);var b=second.get(15,java.util.concurrent.TimeUnit.SECONDS);
            assertEquals(Set.of(201,409),Set.of(a.statusCode(),b.statusCode()));assertEquals(1,countWeights());
            var winner=json.readTree((a.statusCode()==201?a:b).body()).get("weight");
            var conflict=json.readTree((a.statusCode()==409?a:b).body());assertEquals("VERSION_CONFLICT",conflict.get("code").asText());assertEquals(winner,conflict.get("currentItem"));
            var actual=assertStatus(200,send("GET","/api/profile/weight?from=2020-01-03&to=2020-01-03",null));assertEquals(1,actual.size());assertEquals(winner,actual.get(0));assertEquals(0,winner.get("version").asInt());
        }
    }
}
