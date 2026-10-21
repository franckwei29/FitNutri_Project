package com.fitnutri.backend.qa;

import com.fitnutri.backend.support.TestDatabase;
import org.junit.jupiter.api.*;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.*;
import static org.junit.jupiter.api.Assertions.*;
@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT) @ActiveProfiles("integration-test")
class QaUnavailableHttpIntegrationTest {
 static final TestDatabase DB=TestDatabase.create();static{Runtime.getRuntime().addShutdownHook(new Thread(DB::close));}
 @DynamicPropertySource static void db(DynamicPropertyRegistry r){r.add("spring.datasource.url",DB::url);r.add("spring.datasource.username",DB::username);r.add("spring.datasource.password",DB::password);}
 @LocalServerPort int port;
 @Test void actualHttpChecksCurrentConsentBeforeUnconfigured503AndLeavesOrdinaryRecordsAvailable()throws Exception{try(var client=QaHttpClient.registered(port,false)){var request=client.question("General walking information",null);var denied=client.send("POST","/api/qa/messages",request);assertEquals(403,denied.statusCode(),denied.body());assertEquals("QA_RESTRICTED",client.body(denied).get("code").asText());assertEquals(200,client.send("GET","/api/summary/daily?date=2020-01-02&zone=Australia%2FSydney",null).statusCode());client.consent(true);var unavailable=client.send("POST","/api/qa/messages",request);assertEquals(503,unavailable.statusCode(),unavailable.body());assertEquals("FEATURE_NOT_CONFIGURED",client.body(unavailable).get("code").asText());client.consent(false);assertEquals(403,client.send("POST","/api/qa/messages",request).statusCode());}}
}
