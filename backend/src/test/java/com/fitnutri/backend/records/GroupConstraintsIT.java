package com.fitnutri.backend.records;
import com.fitnutri.backend.support.*;
import java.sql.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class GroupConstraintsIT {
 TestDatabase database()throws Exception{var db=TestDatabase.create();db.flywayAtVersion("13").migrate();try(var c=db.connect();var s=c.createStatement()){
  UxV3Fixtures.usersAndDatedWeights(c);s.execute("INSERT INTO food_reference(public_food_key,dataset_id,display_name) VALUES('SYNTHETIC-GROUP',1,'Synthetic group reference')");
 }return db;}
 void dish(Statement s)throws Exception{
  s.execute("INSERT INTO food_dish_record(user_id,client_entry_id,name,recorded_date,meal_type,input_mode,portion_input_kind,portion_numerator,portion_denominator,portion_input_json,source) VALUES(1,gen_random_uuid(),'Synthetic dish','2026-10-17','LUNCH','PERSONAL','PERSONAL',1,1,'{}','MANUAL')");
  s.execute("INSERT INTO food_record(user_id,created_at,recorded_at,recorded_date,client_entry_id,source,food_name,quantity_grams,meal_type,food_reference_id,data_quality,dish_id,dish_position,ingredient_input_grams,nutrition_policy) VALUES(1,now(),now(),'2026-10-17',gen_random_uuid(),'MANUAL','Synthetic ingredient',100,'LUNCH',1,'CURRENT',1,0,100,'RECIPE_PORTION_RATIONAL_V1')");
 }
 void workout(Statement s)throws Exception{
  s.execute("INSERT INTO exercise_option(option_key,revision,active,category,period_kind,display_name,mapping_kind,mapping_evidence,review_status) VALUES('synthetic-strength','test',true,'STRENGTH','STRENGTH','Synthetic strength','UNAVAILABLE','{\"reason\":\"Synthetic no reference\"}','REVIEWED')");
  s.execute("INSERT INTO workout_record(user_id,client_entry_id,title,recorded_date,source) VALUES(1,gen_random_uuid(),'Synthetic workout','2026-10-17','MANUAL')");
  s.execute("INSERT INTO exercise_record(user_id,created_at,recorded_at,recorded_date,client_entry_id,source,exercise_type,duration_minutes,data_quality,estimate_status,exercise_option_id,option_name_snapshot,period_kind,workout_id,period_position) VALUES(1,now(),now(),'2026-10-17',gen_random_uuid(),'MANUAL','Synthetic strength',50,'CURRENT','NO_APPLICABLE_REFERENCE',1,'Synthetic strength','STRENGTH',1,0)");
 }
 @Test void dishDateMealLifecycleAreDeferredAndAtomic()throws Exception{
  try(var db=database();var c=db.connect();var s=c.createStatement()){
   c.setAutoCommit(false);dish(s);c.commit();
   s.execute("UPDATE food_dish_record SET recorded_date='2026-10-16' WHERE id=1");assertEquals("23514",assertThrows(SQLException.class,c::commit).getSQLState());c.rollback();
   s.execute("UPDATE food_dish_record SET recorded_date='2026-10-16',meal_type='DINNER' WHERE id=1");s.execute("UPDATE food_record SET recorded_date='2026-10-16',meal_type='DINNER' WHERE dish_id=1");c.commit();
   s.execute("UPDATE food_dish_record SET deleted_at=now() WHERE id=1");assertEquals("23514",assertThrows(SQLException.class,c::commit).getSQLState());c.rollback();
   s.execute("UPDATE food_dish_record SET deleted_at=now() WHERE id=1");s.execute("UPDATE food_record SET deleted_at=now() WHERE dish_id=1");c.commit();
   try(var r=s.executeQuery("SELECT count(*) FROM food_record WHERE deleted_at IS NULL")){assertTrue(r.next());assertEquals(0,r.getInt(1));}
  }
 }
 @Test void emptyGroupAndChildOnlyDateChangesFailWithoutPartialState()throws Exception{
  try(var db=database();var c=db.connect();var s=c.createStatement()){
   c.setAutoCommit(false);dish(s);c.commit();
   s.execute("UPDATE food_record SET meal_type='DINNER' WHERE id=1");assertEquals("23514",assertThrows(SQLException.class,c::commit).getSQLState());c.rollback();
   s.execute("UPDATE food_record SET deleted_at=now() WHERE id=1");assertEquals("23514",assertThrows(SQLException.class,c::commit).getSQLState());c.rollback();
   try(var r=s.executeQuery("SELECT meal_type,deleted_at FROM food_record WHERE id=1")){assertTrue(r.next());assertEquals("LUNCH",r.getString(1));assertNull(r.getObject(2));}
  }
 }
 @Test void workoutDateLifecycleAndTotalMinutesAreConsistent()throws Exception{
  try(var db=database();var c=db.connect();var s=c.createStatement()){
   c.setAutoCommit(false);workout(s);c.commit();
   s.execute("UPDATE workout_record SET recorded_date='2026-10-16' WHERE id=1");assertEquals("23514",assertThrows(SQLException.class,c::commit).getSQLState());c.rollback();
   s.execute("UPDATE exercise_record SET duration_minutes=900 WHERE id=1");
   s.execute("INSERT INTO exercise_record(user_id,created_at,recorded_at,recorded_date,client_entry_id,source,exercise_type,duration_minutes,data_quality,estimate_status,exercise_option_id,option_name_snapshot,period_kind,workout_id,period_position) SELECT user_id,now(),now(),recorded_date,gen_random_uuid(),source,exercise_type,600,data_quality,estimate_status,exercise_option_id,option_name_snapshot,period_kind,workout_id,1 FROM exercise_record WHERE id=1");
   assertEquals("23514",assertThrows(SQLException.class,c::commit).getSQLState());c.rollback();
   s.execute("UPDATE workout_record SET deleted_at=now() WHERE id=1");s.execute("UPDATE exercise_record SET deleted_at=now() WHERE workout_id=1");c.commit();
  }
 }
 @Test void identitiesStayImmutableAndCrossDomainUuidCannotBypassOwnerNamespace()throws Exception{
  try(var db=database();var c=db.connect();var s=c.createStatement()){
   c.setAutoCommit(false);dish(s);workout(s);c.commit();
   assertEquals("23514",assertThrows(SQLException.class,()->s.execute("UPDATE food_record SET client_entry_id=gen_random_uuid() WHERE id=1")).getSQLState());c.rollback();
   assertEquals("23514",assertThrows(SQLException.class,()->s.execute("UPDATE food_dish_record SET user_id=2 WHERE id=1")).getSQLState());c.rollback();
   s.execute("INSERT INTO food_record(user_id,created_at,recorded_at,recorded_date,client_entry_id,source,food_name,quantity_grams,meal_type) SELECT user_id,now(),now(),recorded_date,client_entry_id,source,'Synthetic duplicate UUID',1,'LUNCH' FROM workout_record WHERE id=1");
   assertEquals("23505",assertThrows(SQLException.class,c::commit).getSQLState());c.rollback();
  }
 }
 private com.fasterxml.jackson.databind.JsonNode preflight(TestDatabase db,boolean pass)throws Exception {
  String container=System.getenv("TEST_DB_DOCKER_CONTAINER");assertNotNull(container);
  String name;try(var c=db.connect()){name=c.getCatalog();}
  var process=new ProcessBuilder("python3",java.nio.file.Path.of("..","scripts","preflight_ux_v3.py").toString(),"--container",container,"--user",db.username(),"--database",name).redirectErrorStream(true).start();
  String output=new String(process.getInputStream().readAllBytes(),java.nio.charset.StandardCharsets.UTF_8);
  assertTrue(process.waitFor(20,java.util.concurrent.TimeUnit.SECONDS));assertEquals(pass?0:2,process.exitValue(),output);
  assertFalse(output.contains("PRIVATE_RECIPE_SENTINEL"));return new com.fasterxml.jackson.databind.ObjectMapper().readTree(output);
 }
 @Test void actualReadOnlyPreflightChecksCurrentSchemaWithoutChangingData()throws Exception {
  try(var db=database();var c=db.connect();var s=c.createStatement()){
   c.setAutoCommit(false);dish(s);workout(s);c.commit();
   var result=preflight(db,true);assertEquals("PASS",result.get("status").asText());assertTrue(result.get("readOnly").asBoolean());assertFalse(result.get("migrationApplied").asBoolean());
   try(var r=s.executeQuery("SELECT count(*),sum(quantity_grams) FROM food_record")){assertTrue(r.next());assertEquals(1,r.getInt(1));assertEquals(0,new java.math.BigDecimal("100").compareTo(r.getBigDecimal(2)));}
  }
 }
 @Test void existingEmptyGroupIsReportedAndStopsMigrationWithoutSilentRepair()throws Exception {
  try(var db=TestDatabase.create()){
   db.flywayAtVersion("12").migrate();try(var c=db.connect();var s=c.createStatement()){
    UxV3Fixtures.usersAndDatedWeights(c);
    s.execute("INSERT INTO food_dish_record(user_id,client_entry_id,name,recorded_date,meal_type,input_mode,portion_input_kind,portion_numerator,portion_denominator,portion_input_json,source) VALUES(1,gen_random_uuid(),'PRIVATE_RECIPE_SENTINEL','2026-10-17','LUNCH','PERSONAL','PERSONAL',1,1,'{}','MANUAL')");
   }
   var result=preflight(db,false);assertEquals("BLOCKED_DATA",result.get("status").asText());assertTrue(result.get("issues").toString().contains("dishConsistency"));
   assertThrows(org.flywaydb.core.api.FlywayException.class,()->db.flywayAtVersion("13").migrate());
   try(var c=db.connect();var s=c.createStatement();var r=s.executeQuery("SELECT count(*),(SELECT max(version::integer) FROM flyway_schema_history WHERE success) FROM food_dish_record")){assertTrue(r.next());assertEquals(1,r.getInt(1));assertEquals(12,r.getInt(2));}
  }
 }
}
