package com.fitnutri.backend.security;
import ch.qos.logback.classic.*;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.fasterxml.jackson.databind.*;
import com.fitnutri.backend.support.TestDatabase;
import com.fitnutri.backend.user.*;
import jakarta.persistence.EntityManagerFactory;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.*;
import java.net.*;
import java.net.http.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.*;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.*;
import org.springframework.test.context.*;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.bind.annotation.*;
import static org.junit.jupiter.api.Assertions.*;
@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT) @ActiveProfiles("integration-test") @Import(RuntimePrivacyHttpIntegrationTest.Probes.class)
class RuntimePrivacyHttpIntegrationTest {
    static final TestDatabase DB=TestDatabase.create();static{Runtime.getRuntime().addShutdownHook(new Thread(DB::close));}
    @DynamicPropertySource static void db(DynamicPropertyRegistry r){r.add("spring.datasource.url",DB::url);r.add("spring.datasource.username",DB::username);r.add("spring.datasource.password",DB::password);}
    @LocalServerPort int port;@Autowired AuthService auth;@Autowired UserRepository users;
    final ObjectMapper json=new ObjectMapper();HttpClient client;String csrf,email;
    final Logger root=(Logger)LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);final ListAppender<ILoggingEvent> logs=new ListAppender<>();
    @BeforeEach void account() throws Exception {email="privacy-"+UUID.randomUUID()+"@example.test";auth.register(new RegisterRequest(email,"Synthetic123","Synthetic privacy test"));client=HttpClient.newBuilder().cookieHandler(new CookieManager(null,CookiePolicy.ACCEPT_ALL)).build();csrf=json.readTree(send("GET","/api/auth/csrf",null).body()).get("token").asText();assertEquals(200,send("POST","/api/auth/login",Map.of("email",email,"password","Synthetic123")).statusCode());csrf=json.readTree(send("GET","/api/auth/csrf",null).body()).get("token").asText();logs.start();root.addAppender(logs);}
    @AfterEach void cleanup(){root.detachAppender(logs);logs.stop();}
    HttpResponse<String> send(String method,String path,Object data) throws Exception {var req=HttpRequest.newBuilder(URI.create("http://127.0.0.1:"+port+path));if(csrf!=null)req.header("X-CSRF-TOKEN",csrf);if(data!=null)req.header("Content-Type","application/json");return client.send(req.method(method,data==null?HttpRequest.BodyPublishers.noBody():HttpRequest.BodyPublishers.ofString(json.writeValueAsString(data))).build(),HttpResponse.BodyHandlers.ofString());}
    @Test void securityRejectionSharesHeaderAndBodyIdWithoutLoggingIdentity() throws Exception {client=HttpClient.newHttpClient();csrf=null;var r=send("GET","/api/profile",null);assertEquals(401,r.statusCode(),r.body());var body=json.readTree(r.body());assertEquals(body.get("requestId").asText(),r.headers().firstValue("X-Request-Id").orElseThrow());String text=logs.list.stream().map(ILoggingEvent::getFormattedMessage).reduce("",(a,b)->a+"\n"+b);assertTrue(text.contains("status=401"));assertTrue(text.contains("requestId="+body.get("requestId").asText()));assertFalse(text.contains(email));}
    @Test void actualMvcRequestHasNoEntityManagerBoundOutsideServiceTransaction() throws Exception {var r=send("GET","/api/test/privacy/context",null);assertEquals(200,r.statusCode(),r.body());assertFalse(json.readTree(r.body()).get("requestBoundEntityManager").asBoolean());UUID.fromString(r.headers().firstValue("X-Request-Id").orElseThrow());}
    @Test void internalErrorAndStructuredLogAreCorrelatedWithoutPrivateExceptionText() throws Exception {var r=send("GET","/api/test/privacy/fail?health=synthetic-private-health",null);assertEquals(500,r.statusCode(),r.body());var body=json.readTree(r.body());assertEquals("INTERNAL_ERROR",body.get("code").asText());assertEquals(body.get("requestId").asText(),r.headers().firstValue("X-Request-Id").orElseThrow());String text=logs.list.stream().map(ILoggingEvent::getFormattedMessage).reduce("",(a,b)->a+"\n"+b);assertTrue(text.contains("operation=GET /api/test/privacy/fail"));assertTrue(text.contains("status=500"));assertFalse(text.contains("synthetic-private"));assertFalse(r.body().contains("synthetic-private"));assertFalse(text.contains(email));}
    @Test void containerErrorDoesNotDiscloseMessageTraceOrBindingDetailsEvenWhenRequested() throws Exception {var r=send("GET","/api/test/privacy/container-error?trace=true&message=true&errors=true",null);assertEquals(500,r.statusCode(),r.body());var body=json.readTree(r.body());assertFalse(body.has("message"));assertFalse(body.has("trace"));assertFalse(body.has("errors"));assertFalse(r.body().contains("synthetic-private"));}
    @Test void realUniqueConstraintFailureKeepsOriginalAccountAndNeverLogsFailingRowMaterial() throws Exception {long before=users.count();var r=send("POST","/api/test/privacy/constraint",null);assertEquals(409,r.statusCode(),r.body());assertEquals("CONSTRAINT_CONFLICT",json.readTree(r.body()).get("code").asText());assertEquals(before,users.count());String text=logs.list.stream().map(ILoggingEvent::getFormattedMessage).reduce("",(a,b)->a+"\n"+b);assertTrue(text.contains("status=409"));assertFalse(text.contains("synthetic-private"));assertFalse(text.contains("Failing row"));assertFalse(text.contains("insert into"));}
    @Test void actualMissingApiAndUnsupportedMethodAreJson404And405RatherThan500OrSpaHtml() throws Exception {
        var missing=send("GET","/api/does-not-exist",null);assertEquals(404,missing.statusCode(),missing.body());assertEquals("NOT_FOUND",json.readTree(missing.body()).get("code").asText());assertEquals("no-store",missing.headers().firstValue("Cache-Control").orElseThrow());assertTrue(missing.headers().firstValue("Content-Type").orElseThrow().contains("application/json"));
        var method=send("POST","/api/auth/me",Map.of());assertEquals(405,method.statusCode(),method.body());assertEquals("REQUEST_REJECTED",json.readTree(method.body()).get("code").asText());assertFalse(method.body().contains("<!doctype"));
    }
    @TestConfiguration static class Probes {@Bean ProbeController probe(EntityManagerFactory emf,UserRepository users){return new ProbeController(emf,users);}}
    @org.springframework.boot.test.context.TestComponent @RestController static class ProbeController {
        final EntityManagerFactory emf;final UserRepository users;ProbeController(EntityManagerFactory emf,UserRepository users){this.emf=emf;this.users=users;}
        @GetMapping("/api/test/privacy/context") Map<String,Boolean> context(){return Map.of("requestBoundEntityManager",TransactionSynchronizationManager.hasResource(emf));}
        @GetMapping("/api/test/privacy/fail") void fail(){throw new IllegalStateException("synthetic-private-password-provider-sql");}
        @GetMapping("/api/test/privacy/container-error") void error(HttpServletRequest request,HttpServletResponse response) throws Exception {request.setAttribute(RequestDispatcher.ERROR_STATUS_CODE,500);request.setAttribute(RequestDispatcher.ERROR_MESSAGE,"synthetic-private-container-message");request.setAttribute(RequestDispatcher.ERROR_EXCEPTION,new IllegalStateException("synthetic-private-container-stack"));request.setAttribute(RequestDispatcher.ERROR_REQUEST_URI,"/api/test/privacy/container-error");request.getRequestDispatcher("/error").forward(request,response);}
        @PostMapping("/api/test/privacy/constraint") void constraint(java.security.Principal principal){User first=users.findByEmail(principal.getName()).orElseThrow();User duplicate=new User();duplicate.setEmail(first.getEmail());duplicate.setDisplayName("synthetic-private-health-answer");duplicate.setPasswordHash("synthetic-private-hash-material");users.saveAndFlush(duplicate);}
    }
}
