package com.fitnutri.backend.admin;

import com.fasterxml.jackson.databind.*;
import com.fitnutri.backend.common.BusinessException;
import com.fitnutri.backend.support.TestDatabase;
import com.fitnutri.backend.user.*;
import java.net.*;
import java.net.http.*;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.*;
import static org.junit.jupiter.api.Assertions.*;

/** Actual cookies/CSRF/MVC + real PostgreSQL locking, no mocked persistence. */
@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT) @ActiveProfiles("integration-test")
class AdminHttpIntegrationTest {
    static final TestDatabase DB=TestDatabase.create();
    static{Runtime.getRuntime().addShutdownHook(new Thread(DB::close));}
    @DynamicPropertySource static void db(DynamicPropertyRegistry r){r.add("spring.datasource.url",DB::url);r.add("spring.datasource.username",DB::username);r.add("spring.datasource.password",DB::password);}
    @LocalServerPort int port;
    @Autowired UserRepository users;@Autowired PasswordEncoder passwords;@Autowired AdminService service;
    final ObjectMapper json=new ObjectMapper();
    User admin,target;Client client;
    User create(User.Role role){User u=new User();u.setRole(role);u.setEmail("admin-test-"+UUID.randomUUID()+"@example.test");u.setDisplayName("Synthetic administrator test");u.setPasswordHash(passwords.encode("Synthetic123"));return users.saveAndFlush(u);}
    @BeforeEach void arrange() throws Exception {admin=create(User.Role.ADMIN);target=create(User.Role.USER);client=new Client(admin);}
    class Client {
        final HttpClient http=HttpClient.newBuilder().cookieHandler(new CookieManager(null,CookiePolicy.ACCEPT_ALL)).build();String csrf;
        Client(User user) throws Exception {csrf=body(send("GET","/api/auth/csrf",null)).get("token").asText();assertEquals(200,send("POST","/api/auth/login",Map.of("email",user.getEmail(),"password","Synthetic123")).statusCode());csrf=body(send("GET","/api/auth/csrf",null)).get("token").asText();}
        HttpResponse<String> send(String method,String path,Object data) throws Exception {var b=HttpRequest.newBuilder(URI.create("http://127.0.0.1:"+port+path));if(csrf!=null)b.header("X-CSRF-TOKEN",csrf);if(data!=null)b.header("Content-Type","application/json");return http.send(b.method(method,data==null?HttpRequest.BodyPublishers.noBody():HttpRequest.BodyPublishers.ofString(json.writeValueAsString(data))).build(),HttpResponse.BodyHandlers.ofString());}
    }
    JsonNode body(HttpResponse<String> r) throws Exception {return json.readTree(r.body());}
    String path(User u,String action){return "/api/admin/users/"+u.getId()+"/"+action;}
    @Test void boundedSearchMatchesWholeServerDirectoryAndHasOnlySixPublicFields() throws Exception {
        var response=client.send("GET","/api/admin/users?query="+target.getEmail()+"&page=0&size=1",null);assertEquals(200,response.statusCode(),response.body());assertEquals("no-store",response.headers().firstValue("Cache-Control").orElseThrow());var result=body(response);assertEquals(1,result.get("totalMatches").asInt());assertFalse(result.get("hasMore").asBoolean());var row=result.get("results").get(0);assertEquals(Set.of("id","email","displayName","role","enabled","version"),json.convertValue(row,Map.class).keySet());assertEquals(target.getId().toString(),row.get("id").asText());assertTrue(row.get("id").isTextual());assertEquals(400,client.send("GET","/api/admin/users?size=101",null).statusCode());assertEquals(400,client.send("GET","/api/admin/users?page=-1",null).statusCode());
        assertEquals(0,body(client.send("GET","/api/admin/users?query=%25",null)).get("totalMatches").asInt());
        var page0=body(client.send("GET","/api/admin/users?size=1&page=0",null));var page1=body(client.send("GET","/api/admin/users?size=1&page=1",null));assertTrue(page0.get("hasMore").asBoolean());assertNotEquals(page0.get("results").get(0).get("id"),page1.get("results").get(0).get("id"));
    }
    @Test void regularUserCannotListChangeResetOrReadOtherPrivateResources() throws Exception {var ordinary=new Client(target);for(String action:List.of("deactivate","reactivate"))assertEquals(403,ordinary.send("PATCH",path(admin,action),Map.of("expectedVersion",0)).statusCode());assertEquals(403,ordinary.send("GET","/api/admin/users",null).statusCode());assertEquals(403,ordinary.send("POST",path(admin,"reset-password"),Map.of("expectedVersion",0,"newPassword","NewSynthetic123")).statusCode());assertEquals(403,client.send("GET","/api/profile",null).statusCode());assertEquals(403,client.send("GET","/api/records/food?from=2020-01-01&to=2020-01-01",null).statusCode());assertEquals(0,users.findById(admin.getId()).orElseThrow().getVersion());}
    @Test void deactivateRechecksExistingSessionAndReactivateRetainsOwnedData() throws Exception {
        try(var c=DB.connect();var p=c.prepareStatement("INSERT INTO weight_entry(user_id,weight_kg,entry_date,created_at) VALUES (?,70,'2020-01-01',CURRENT_TIMESTAMP)")){p.setLong(1,target.getId());p.executeUpdate();}
        var ordinary=new Client(target);assertEquals(200,ordinary.send("GET","/api/auth/me",null).statusCode());var disabled=client.send("PATCH",path(target,"deactivate"),Map.of("expectedVersion",0));assertEquals(200,disabled.statusCode(),disabled.body());assertFalse(body(disabled).get("enabled").asBoolean());assertEquals(1,body(disabled).get("version").asInt());assertEquals(401,ordinary.send("GET","/api/auth/me",null).statusCode());assertEquals(409,client.send("PATCH",path(target,"reactivate"),Map.of("expectedVersion",0)).statusCode());var restored=client.send("PATCH",path(target,"reactivate"),Map.of("expectedVersion",1));assertEquals(200,restored.statusCode(),restored.body());assertEquals(2,body(restored).get("version").asInt());assertTrue(body(restored).get("enabled").asBoolean());
        try(var c=DB.connect();var p=c.prepareStatement("SELECT weight_kg,entry_date,version FROM weight_entry WHERE user_id=?")){p.setLong(1,target.getId());try(var r=p.executeQuery()){assertTrue(r.next());assertEquals("70.00",r.getBigDecimal(1).toPlainString());assertEquals("2020-01-01",r.getDate(2).toString());assertEquals(0,r.getLong(3));assertFalse(r.next());}}
    }
    @Test void selfProtectionAndMissingIdentityHaveNoWrite() throws Exception {var denied=client.send("PATCH",path(admin,"deactivate"),Map.of("expectedVersion",0));assertEquals(409,denied.statusCode(),denied.body());assertEquals("ACCOUNT_PROTECTED",body(denied).get("code").asText());assertEquals(404,client.send("PATCH","/api/admin/users/9223372036854775807/deactivate",Map.of("expectedVersion",0)).statusCode());assertEquals(0,users.findById(admin.getId()).orElseThrow().getVersion());}
    @Test void resetRequiresCurrentVersionAndActualNewPasswordWorks() throws Exception {var result=client.send("POST",path(target,"reset-password"),Map.of("expectedVersion",0,"newPassword","NewSynthetic123"));assertEquals(204,result.statusCode(),result.body());assertTrue(result.body().isEmpty());var saved=users.findById(target.getId()).orElseThrow();assertEquals(1,saved.getVersion());assertTrue(passwords.matches("NewSynthetic123",saved.getPasswordHash()));assertFalse(passwords.matches("Synthetic123",saved.getPasswordHash()));assertEquals(409,client.send("POST",path(target,"reset-password"),Map.of("expectedVersion",0,"newPassword","AnotherSynthetic123")).statusCode());assertEquals(1,users.findById(target.getId()).orElseThrow().getVersion());
        var probe=HttpClient.newBuilder().cookieHandler(new CookieManager(null,CookiePolicy.ACCEPT_ALL)).build();var url="http://127.0.0.1:"+port;var token=body(probe.send(HttpRequest.newBuilder(URI.create(url+"/api/auth/csrf")).GET().build(),HttpResponse.BodyHandlers.ofString())).get("token").asText();var login=probe.send(HttpRequest.newBuilder(URI.create(url+"/api/auth/login")).header("X-CSRF-TOKEN",token).header("Content-Type","application/json").POST(HttpRequest.BodyPublishers.ofString(json.writeValueAsString(Map.of("email",target.getEmail(),"password","NewSynthetic123")))).build(),HttpResponse.BodyHandlers.ofString());assertEquals(200,login.statusCode(),login.body());
    }
    @Test void missingCsrfCannotWriteEvenForAdministrator() throws Exception {client.csrf=null;assertEquals(403,client.send("PATCH",path(target,"deactivate"),Map.of("expectedVersion",0)).statusCode());assertTrue(users.findById(target.getId()).orElseThrow().isEnabled());}
    @Test void concurrentMutualAdminDeactivationRechecksAuthorityAfterOrderedLocks() throws Exception {var other=create(User.Role.ADMIN);var start=new CountDownLatch(1);try(var pool=Executors.newFixedThreadPool(2)){var a=pool.submit(()->change(start,admin,other));var b=pool.submit(()->change(start,other,admin));start.countDown();assertEquals(Set.of("UPDATED","UNAUTHENTICATED"),new HashSet<>(List.of(a.get(15,TimeUnit.SECONDS),b.get(15,TimeUnit.SECONDS))));assertEquals(1,(users.findById(admin.getId()).orElseThrow().isEnabled()?1:0)+(users.findById(other.getId()).orElseThrow().isEnabled()?1:0));}}
    @Test void concurrentChangesOfOneVersionHaveOneWinnerAndNoSilentOverwrite() throws Exception {var start=new CountDownLatch(1);try(var pool=Executors.newFixedThreadPool(2)){var a=pool.submit(()->change(start,admin,target));var b=pool.submit(()->change(start,admin,target));start.countDown();assertEquals(Set.of("UPDATED","VERSION_CONFLICT"),new HashSet<>(List.of(a.get(15,TimeUnit.SECONDS),b.get(15,TimeUnit.SECONDS))));assertEquals(1,users.findById(target.getId()).orElseThrow().getVersion());}}
    String change(CountDownLatch start,User actor,User target) throws Exception {start.await();try{service.changeStatus(new UsernamePasswordAuthenticationToken(actor.getEmail(),null,List.of()),target.getId(),new ChangeAccountStatusRequest(0L),false);return "UPDATED";}catch(BusinessException e){return e.code();}}

    @Test void concurrentHttpAdminChangesReturnOneVersionConflictWithoutOverwritingTarget() throws Exception {
        var secondSession=new Client(admin);var start=new CountDownLatch(1);
        try(var pool=Executors.newFixedThreadPool(2)) {
            var first=pool.submit(()->{start.await();return client.send("PATCH",path(target,"deactivate"),Map.of("expectedVersion",0));});
            var second=pool.submit(()->{start.await();return secondSession.send("PATCH",path(target,"deactivate"),Map.of("expectedVersion",0));});start.countDown();
            var a=first.get(15,TimeUnit.SECONDS);var b=second.get(15,TimeUnit.SECONDS);assertEquals(Set.of(200,409),Set.of(a.statusCode(),b.statusCode()));
            var winner=body(a.statusCode()==200?a:b);assertFalse(winner.get("enabled").asBoolean());assertEquals(1,winner.get("version").asInt());
            assertEquals("VERSION_CONFLICT",body(a.statusCode()==409?a:b).get("code").asText());
            var actual=users.findById(target.getId()).orElseThrow();assertFalse(actual.isEnabled());assertEquals(1,actual.getVersion());assertEquals(User.Role.USER,actual.getRole());
            assertEquals(Set.of("id","email","displayName","role","enabled","version"),json.convertValue(winner,Map.class).keySet());
        }
    }
}
