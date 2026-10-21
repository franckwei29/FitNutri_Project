package com.fitnutri.backend.security;

import com.fitnutri.backend.support.TestDatabase;
import com.fitnutri.backend.user.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.*;
import java.net.http.*;
import java.util.List;
import java.util.concurrent.*;
import java.util.UUID;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("integration-test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class SessionCsrfIntegrationTest {
    private static final TestDatabase DB=TestDatabase.create();
    static {Runtime.getRuntime().addShutdownHook(new Thread(DB::close));}
    @DynamicPropertySource static void db(DynamicPropertyRegistry r) {r.add("spring.datasource.url",DB::url);r.add("spring.datasource.username",DB::username);r.add("spring.datasource.password",DB::password);}
    @LocalServerPort private int port;
    @Autowired private UserRepository users;
    @Autowired private AuthService authService;
    private final ObjectMapper json=new ObjectMapper();
    private CookieManager cookies;
    private HttpClient client;
    @BeforeEach void arrange() {cookies=new CookieManager(null,CookiePolicy.ACCEPT_ALL);client=HttpClient.newBuilder().cookieHandler(cookies).build();}
    private HttpResponse<String> get(String path) throws Exception {return client.send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:"+port+path)).GET().build(),HttpResponse.BodyHandlers.ofString());}
    private HttpResponse<String> post(String path,String body,String token) throws Exception {
        var request=HttpRequest.newBuilder(URI.create("http://127.0.0.1:"+port+path)).header("Content-Type","application/json");
        if(token!=null) request.header("X-CSRF-TOKEN",token);
        return client.send(request.POST(HttpRequest.BodyPublishers.ofString(body)).build(),HttpResponse.BodyHandlers.ofString());
    }
    private String csrf() throws Exception {var response=get("/api/auth/csrf");assertEquals(200,response.statusCode());assertTrue(response.headers().firstValue("cache-control").orElse("").contains("no-store"));return json.readTree(response.body()).get("token").asText();}
    private String body(String email,String password) throws Exception {return json.writeValueAsString(java.util.Map.of("email",email,"password",password,"displayName","Synthetic test"));}
    private String loginBody(String email,String password) throws Exception {return json.writeValueAsString(java.util.Map.of("email",email,"password",password));}
    private String uniqueEmail() {return "synthetic-"+UUID.randomUUID()+"@example.test";}
    private String cookie() {return cookies.getCookieStore().getCookies().stream().filter(c->c.getName().equals("JSESSIONID")).findFirst().orElseThrow().getValue();}

    @Test @Order(1) void firstPublicRegistrationIsUserAndDoesNotLoginOrExposeHash() throws Exception {
        assertEquals(0,users.count());
        String email=uniqueEmail();var response=post("/api/auth/register",body(email,"Synthetic123"),csrf());
        assertEquals(201,response.statusCode());JsonNode user=json.readTree(response.body());
        assertEquals("USER",user.get("role").asText());assertTrue(user.get("id").isTextual());assertTrue(user.get("enabled").asBoolean());assertEquals(0,user.get("version").asInt());
        assertFalse(response.body().contains("password"));assertEquals(401,get("/api/auth/me").statusCode());
    }
    @Test @Order(2) void emailNormalisationAndByteLimitAreEnforced() throws Exception {
        String email=uniqueEmail(), token=csrf();
        assertEquals(201,post("/api/auth/register",body("  "+email.toUpperCase(java.util.Locale.ROOT)+"  ","Synthetic123"),token).statusCode());
        assertEquals(409,post("/api/auth/register",body(email,"Synthetic123"),token).statusCode());
        assertEquals(400,post("/api/auth/register",body(uniqueEmail(),"é".repeat(36)+"A1"),token).statusCode());
        assertEquals(201,post("/api/auth/register",body(uniqueEmail(),"é".repeat(35)+"A1"),token).statusCode());
    }
    @Test @Order(3) void roleInjectionCannotCreateAdminAndMissingCsrfIsRejectedBeforeWrite() throws Exception {
        String email=uniqueEmail();var without=post("/api/auth/register",body(email,"Synthetic123"),null);
        assertEquals(403,without.statusCode());assertEquals("CSRF_INVALID",json.readTree(without.body()).get("code").asText());
        assertTrue(users.findByCanonicalEmail(email).isEmpty());
        var response=post("/api/auth/register",body(email,"Synthetic123").replace("}",",\"role\":\"ADMIN\"}"),csrf());
        assertEquals(201,response.statusCode(),response.body());
        assertEquals("USER",json.readTree(response.body()).get("role").asText());
        assertEquals(User.Role.USER,users.findByCanonicalEmail(email).orElseThrow().getRole());
    }
    @Test @Order(4) void loginRotatesSessionAndCsrfThenLogoutInvalidatesRealCookieSession() throws Exception {
        String email=uniqueEmail(),token=csrf();assertEquals(201,post("/api/auth/register",body(email,"Synthetic123"),token).statusCode());
        String before=cookie();assertEquals(200,post("/api/auth/login",loginBody(email,"Synthetic123"),token).statusCode());
        assertNotEquals(before,cookie());assertEquals(200,get("/api/auth/me").statusCode());
        assertEquals(403,post("/api/auth/logout","{}",token).statusCode());
        assertEquals(403,get("/api/admin/users").statusCode());
        assertEquals(204,post("/api/auth/logout","{}",csrf()).statusCode());assertEquals(401,get("/api/auth/me").statusCode());
    }
    @Test @Order(5) void disabledAccountCannotUseExistingOrNewSessions() throws Exception {
        String email=uniqueEmail(),token=csrf();assertEquals(201,post("/api/auth/register",body(email,"Synthetic123"),token).statusCode());
        assertEquals(200,post("/api/auth/login",loginBody(email,"Synthetic123"),token).statusCode());
        User user=users.findByCanonicalEmail(email).orElseThrow();user.setEnabled(false);users.saveAndFlush(user);
        assertEquals(401,get("/api/auth/me").statusCode());
        assertEquals(401,post("/api/auth/login",loginBody(email,"Synthetic123"),csrf()).statusCode());
    }
    @Test @Order(6) void concurrentCanonicalRegistrationCreatesExactlyOneAccount() throws Exception {
        String email=uniqueEmail();CountDownLatch start=new CountDownLatch(1);
        try(ExecutorService pool=Executors.newFixedThreadPool(2)) {
            Callable<Integer> attempt=()->{start.await();try{authService.register(new RegisterRequest(email,"Synthetic123","Synthetic"));return 201;}catch(com.fitnutri.backend.common.BusinessException e){return e.status().value();}};
            var first=pool.submit(attempt);var second=pool.submit(attempt);start.countDown();
            var codes=new java.util.ArrayList<>(List.of(first.get(15,TimeUnit.SECONDS),second.get(15,TimeUnit.SECONDS)));java.util.Collections.sort(codes);
            assertEquals(List.of(201,409),codes);assertEquals(User.Role.USER,users.findByCanonicalEmail(email).orElseThrow().getRole());
        }
    }
    @Test @Order(7) void controlledAdminBootstrapIsIdempotentAndCannotPromoteAnExistingUser() throws Exception {
        String email=uniqueEmail();var admin=authService.bootstrapAdmin(new RegisterRequest(email,"Synthetic123","Synthetic admin"));
        assertEquals("ADMIN",admin.role());assertEquals(admin.id(),authService.bootstrapAdmin(new RegisterRequest(email,"Another123","Synthetic admin")).id());
        String ordinary=uniqueEmail();authService.register(new RegisterRequest(ordinary,"Synthetic123","Synthetic user"));
        assertThrows(com.fitnutri.backend.common.BusinessException.class,()->authService.bootstrapAdmin(new RegisterRequest(ordinary,"Synthetic123","Synthetic user")));
        assertEquals(200,post("/api/auth/login",loginBody(email,"Synthetic123"),csrf()).statusCode());
        assertEquals(403,get("/api/profile").statusCode());assertEquals(200,get("/api/admin/users").statusCode());
    }

    @Test @Order(8) void changedDatabaseRoleImmediatelyRevokesStaleSessionAuthority() throws Exception {
        String email=uniqueEmail();authService.bootstrapAdmin(new RegisterRequest(email,"Synthetic123","Synthetic"));
        assertEquals(200,post("/api/auth/login",loginBody(email,"Synthetic123"),csrf()).statusCode());
        User account=users.findByCanonicalEmail(email).orElseThrow();account.setRole(User.Role.USER);users.saveAndFlush(account);
        var response=get("/api/admin/users");assertEquals(403,response.statusCode());assertEquals("FORBIDDEN",json.readTree(response.body()).get("code").asText());
        assertEquals("USER",json.readTree(get("/api/auth/me").body()).get("role").asText());
    }
    @Test @Order(9) void anonymousCsrfCookieAndUnauthorisedErrorsUseRealHttpContract() throws Exception {
        var response=get("/api/auth/csrf");assertEquals(200,response.statusCode());
        String header=response.headers().firstValue("set-cookie").orElseThrow();
        assertTrue(header.contains("HttpOnly"));assertTrue(header.contains("SameSite=Lax"));
        assertEquals("X-CSRF-TOKEN",json.readTree(response.body()).get("headerName").asText());
        var missing=get("/api/auth/me");assertEquals(401,missing.statusCode());assertEquals("UNAUTHENTICATED",json.readTree(missing.body()).get("code").asText());
        assertFalse(missing.body().contains("password"));assertFalse(missing.body().contains("SELECT"));
    }
    @Test @Order(10) void badPasswordAndMissingRegistrationFieldsDoNotCreateAnAccount() throws Exception {
        String email=uniqueEmail(),token=csrf();assertEquals(201,post("/api/auth/register",body(email,"Synthetic123"),token).statusCode());
        assertEquals(401,post("/api/auth/login",loginBody(email,"Incorrect123"),token).statusCode());
        var invalid=post("/api/auth/register","{}",token);assertEquals(400,invalid.statusCode());assertEquals("VALIDATION_ERROR",json.readTree(invalid.body()).get("code").asText());
        assertTrue(json.readTree(invalid.body()).get("fieldErrors").size()>0);
    }

    @Test @Order(11) void independentCookieClientsConcurrentlyRegisterOneCanonicalUserThroughHttp() throws Exception {
        String email=uniqueEmail();long before=users.count();var start=new CountDownLatch(1);
        try(var pool=Executors.newFixedThreadPool(2)) {
            java.util.concurrent.Callable<HttpResponse<String>> attempt=()->{
                var independent=HttpClient.newBuilder().cookieHandler(new CookieManager(null,CookiePolicy.ACCEPT_ALL)).build();
                var base="http://127.0.0.1:"+port;
                var csrfResponse=independent.send(HttpRequest.newBuilder(URI.create(base+"/api/auth/csrf")).GET().build(),HttpResponse.BodyHandlers.ofString());
                assertEquals(200,csrfResponse.statusCode());String token=json.readTree(csrfResponse.body()).get("token").asText();
                start.await();
                return independent.send(HttpRequest.newBuilder(URI.create(base+"/api/auth/register")).header("X-CSRF-TOKEN",token).header("Content-Type","application/json").POST(HttpRequest.BodyPublishers.ofString(body("  "+email.toUpperCase(java.util.Locale.ROOT)+"  ","Synthetic123"))).build(),HttpResponse.BodyHandlers.ofString());
            };
            var first=pool.submit(attempt);var second=pool.submit(attempt);start.countDown();
            var a=first.get(15,TimeUnit.SECONDS);var b=second.get(15,TimeUnit.SECONDS);
            assertEquals(java.util.Set.of(201,409),java.util.Set.of(a.statusCode(),b.statusCode()));assertEquals(before+1,users.count());
            assertEquals(User.Role.USER,users.findByCanonicalEmail(email).orElseThrow().getRole());
            var rejected=json.readTree((a.statusCode()==409?a:b).body());assertEquals("EMAIL_ALREADY_REGISTERED",rejected.get("code").asText());
            assertFalse(a.body().contains("password"));assertFalse(b.body().contains("password"));
        }
    }
    @Test @Order(12) void v2FoodRetainsRealSessionCsrfAndRoleBoundaries()throws Exception{
        String email=uniqueEmail();assertEquals(201,post("/api/auth/register",body(email,"Synthetic123"),csrf()).statusCode());assertEquals(200,post("/api/auth/login",loginBody(email,"Synthetic123"),csrf()).statusCode());
        assertEquals(403,post("/api/v2/food/batches","{}",null).statusCode());assertEquals(400,post("/api/v2/food/batches","{}",csrf()).statusCode());var recent=get("/api/v2/food/recent");assertEquals(200,recent.statusCode());assertTrue(recent.headers().firstValue("cache-control").orElse("").contains("no-store"));assertTrue(json.readTree(recent.body()).get("results").isEmpty());
        String admin=uniqueEmail();authService.bootstrapAdmin(new RegisterRequest(admin,"Synthetic123","Synthetic V2 admin"));assertEquals(200,post("/api/auth/login",loginBody(admin,"Synthetic123"),csrf()).statusCode());assertEquals(403,get("/api/v2/food/recent").statusCode());assertEquals(403,post("/api/v2/food/batches","{}",csrf()).statusCode());
    }
}
