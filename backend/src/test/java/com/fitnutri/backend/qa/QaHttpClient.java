package com.fitnutri.backend.qa;

import com.fasterxml.jackson.databind.*;
import java.net.*;
import java.net.http.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

/** Owned synthetic Session/CSRF HTTP fixture only; no real account or provider credential. */
final class QaHttpClient implements AutoCloseable {
 final HttpClient http=HttpClient.newBuilder().cookieHandler(new CookieManager(null,CookiePolicy.ACCEPT_ALL)).build();final int port;final ObjectMapper json=new ObjectMapper();String csrf,email,userId;
 QaHttpClient(int port)throws Exception{this.port=port;csrf=body(send("GET","/api/auth/csrf",null)).get("token").asText();}
 HttpResponse<String> send(String method,String path,Object value)throws Exception{var b=HttpRequest.newBuilder(URI.create("http://127.0.0.1:"+port+path));if(csrf!=null)b.header("X-CSRF-TOKEN",csrf);if(value!=null)b.header("Content-Type","application/json");return http.send(b.method(method,value==null?HttpRequest.BodyPublishers.noBody():HttpRequest.BodyPublishers.ofString(json.writeValueAsString(value))).build(),HttpResponse.BodyHandlers.ofString());}
 JsonNode body(HttpResponse<String> response)throws Exception{return json.readTree(response.body());}
 void login(String email)throws Exception{this.email=email;var response=send("POST","/api/auth/login",Map.of("email",email,"password","Synthetic123"));assertEquals(200,response.statusCode(),response.body());csrf=body(send("GET","/api/auth/csrf",null)).get("token").asText();userId=body(send("GET","/api/auth/me",null)).get("id").asText();}
 static QaHttpClient registered(int port,boolean consent)throws Exception{var client=new QaHttpClient(port);String email="qa-"+UUID.randomUUID()+"@example.test";var response=client.send("POST","/api/auth/register",Map.of("email",email,"password","Synthetic123","displayName","Synthetic private QA test"));assertEquals(201,response.statusCode(),response.body());client.login(email);var setup=new LinkedHashMap<String,Object>();setup.put("expectedUserVersion",0);setup.put("expectedProfileVersion",null);setup.put("displayName","Synthetic private QA test");setup.put("birthYear",1990);setup.put("adultConfirmed",true);setup.put("sex","PREFER_NOT_TO_SAY");setup.put("heightCm",170);setup.put("activityLevel","LIGHT");setup.put("goalType",null);setup.put("dailyTargetKj",null);setup.put("energyUnit","KJ");setup.put("healthSafetyAnswer","NO");setup.put("qaConsentGranted",consent);setup.put("zone","Australia/Sydney");setup.put("initialWeight",Map.of("recordDate","2020-01-01","weightKg",70));response=client.send("POST","/api/profile/onboarding",setup);assertEquals(200,response.statusCode(),response.body());return client;}
 void consent(boolean value)throws Exception{var p=body(send("GET","/api/profile",null));var update=new LinkedHashMap<String,Object>();update.put("expectedUserVersion",p.at("/user/version").longValue());update.put("expectedProfileVersion",p.get("profileVersion").longValue());update.put("displayName",p.at("/user/displayName").asText());for(String name:List.of("birthYear","adultConfirmed","sex","heightCm","activityLevel","goalType","dailyTargetKj","energyUnit","healthSafetyAnswer"))update.put(name,json.convertValue(p.get(name),Object.class));update.put("qaConsentGranted",value);update.put("zone","Australia/Sydney");var response=send("PUT","/api/profile",update);assertEquals(200,response.statusCode(),response.body());assertEquals(value,body(response).get("qaConsentGranted").asBoolean());}
 Map<String,Object> question(String text,Object range){var request=new LinkedHashMap<String,Object>();request.put("message",text);request.put("recordRange",range);request.put("zone","Australia/Sydney");return request;}
 @Override public void close(){http.close();}
}
