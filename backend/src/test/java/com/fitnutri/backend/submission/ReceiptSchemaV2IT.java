package com.fitnutri.backend.submission;
import com.fitnutri.backend.support.*;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.*;
import java.sql.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class ReceiptSchemaV2IT {
 final ObjectMapper json=new ObjectMapper();
 String root="22222222-2222-4222-8222-222222222222",leaf="33333333-3333-4333-8333-333333333333";
 ObjectNode proof()throws Exception{return (ObjectNode)json.readTree("{\"schemaVersion\":2,\"roots\":[{\"kind\":\"DISH\",\"id\":\"1\",\"clientEntryId\":\""+root+"\",\"recordDate\":\"2026-10-17\",\"versionAtCreation\":0}],\"leaves\":[{\"kind\":\"FOOD_COMPONENT\",\"id\":\"1\",\"clientEntryId\":\""+leaf+"\",\"rootClientEntryId\":\""+root+"\",\"recordDate\":\"2026-10-17\",\"versionAtCreation\":0}]}");}
 boolean valid(Connection c,String operation,JsonNode proof)throws Exception{try(var s=c.prepareStatement("SELECT receipt_result_is_valid_v2(?,?::jsonb)")){s.setString(1,operation);s.setString(2,proof.toString());try(var r=s.executeQuery()){r.next();return r.getBoolean(1);}}}
 TestDatabase database()throws Exception{var db=TestDatabase.create();db.flywayAtVersion("10").migrate();try(var c=db.connect()){UxV3Fixtures.usersAndDatedWeights(c);}db.flywayAtVersion("11").migrate();return db;}
 @Test void strictTypedProofAcceptsCrossTableNumericIdentityAndPreservesV1()throws Exception{
  try(var db=database();var c=db.connect()){
   assertTrue(valid(c,"FOOD_BATCH_V2",proof()));
   var old=json.readTree("{\"records\":[{\"id\":\"1\",\"clientEntryId\":\""+root+"\",\"recordDate\":\"2020-01-01\",\"versionAtCreation\":0}]}");
   assertTrue(valid(c,"FOOD_BATCH",old));assertFalse(valid(c,"FOOD_BATCH_V2",old));assertFalse(valid(c,"FOOD_BATCH",proof()));
   var quick=proof();((ObjectNode)quick.withArray("roots").get(0)).put("kind","QUICK");var l=(ObjectNode)quick.withArray("leaves").get(0);l.put("kind","ACTIVITY_PERIOD").put("clientEntryId",root);
   assertTrue(valid(c,"EXERCISE_ENTRY_V2",quick));l.put("id","2");assertFalse(valid(c,"EXERCISE_ENTRY_V2",quick));
  }
 }
 @Test void extraMissingWrongParentDuplicateAndInvalidDatesAreRejected()throws Exception{
  try(var db=database();var c=db.connect()){
   var p=proof();p.put("privateRecipe","forbidden");assertFalse(valid(c,"FOOD_BATCH_V2",p));
   p=proof();p.withArray("leaves").removeAll();assertFalse(valid(c,"FOOD_BATCH_V2",p));
   p=proof();((ObjectNode)p.withArray("leaves").get(0)).put("rootClientEntryId",leaf);assertFalse(valid(c,"FOOD_BATCH_V2",p));
   p=proof();p.withArray("leaves").add(p.withArray("leaves").get(0).deepCopy());assertFalse(valid(c,"FOOD_BATCH_V2",p));
   for(String date:new String[]{"2026-02-30","2026-10-16"}){p=proof();((ObjectNode)p.withArray("leaves").get(0)).put("recordDate",date);assertFalse(valid(c,"FOOD_BATCH_V2",p));}
   p=proof();((ObjectNode)p.withArray("roots").get(0)).put("versionAtCreation",1);assertFalse(valid(c,"FOOD_BATCH_V2",p));
   p=proof();for(int i=0;i<20;i++)p.withArray("roots").add(p.withArray("roots").get(0).deepCopy());assertFalse(valid(c,"FOOD_BATCH_V2",p));
  }
 }
 @Test void databaseRejectsPrivateProofAndIncompleteOrMutableCommittedReceipts()throws Exception{
  try(var db=database();var c=db.connect();var s=c.createStatement()){
   var insert=c.prepareStatement("INSERT INTO submission_receipt(user_id,submission_id,operation,request_sha256,result_json,committed_at) VALUES(1,gen_random_uuid(),'FOOD_BATCH_V2',repeat('a',64),?::jsonb,now())");
   insert.setString(1,proof().toString());insert.executeUpdate();
   assertEquals("23514",assertThrows(SQLException.class,()->s.execute("UPDATE submission_receipt SET result_json=result_json||'{\"name\":\"private\"}'")).getSQLState());
   assertEquals("23514",assertThrows(SQLException.class,()->s.execute("DELETE FROM submission_receipt")).getSQLState());
   var bad=proof();bad.put("recipe","not allowed");insert.setString(1,bad.toString());assertEquals("23514",assertThrows(SQLException.class,insert::executeUpdate).getSQLState());
   c.setAutoCommit(false);s.execute("INSERT INTO submission_receipt(user_id,submission_id,operation,request_sha256) VALUES(1,gen_random_uuid(),'FOOD_BATCH_V2',repeat('b',64))");
   assertEquals("23514",assertThrows(SQLException.class,c::commit).getSQLState());c.rollback();
  }
 }
}
