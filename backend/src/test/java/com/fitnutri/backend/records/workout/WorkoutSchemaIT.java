package com.fitnutri.backend.records.workout;
import com.fitnutri.backend.support.*;
import java.sql.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class WorkoutSchemaIT {
 TestDatabase database()throws Exception{var db=TestDatabase.create();db.flywayAtVersion("11").migrate();try(var c=db.connect();var s=c.createStatement()){
  UxV3Fixtures.usersAndDatedWeights(c);
  s.execute("INSERT INTO exercise_record(user_id,created_at,recorded_at,recorded_date,client_entry_id,source,exercise_type,duration_minutes,estimated_kj) VALUES(1,now(),now(),'2026-10-17',gen_random_uuid(),'PHOTO','Synthetic legacy',10,123.456)");
 }db.flywayAtVersion("12").migrate();return db;}
 void options(Statement s)throws Exception{
  s.execute("INSERT INTO exercise_option(option_key,revision,active,category,period_kind,display_name,mapping_kind,mapping_evidence,review_status) VALUES('synthetic-strength','test',true,'STRENGTH','STRENGTH','Synthetic strength','UNAVAILABLE','{\"reason\":\"Synthetic no mapping\"}','REVIEWED')");
  s.execute("INSERT INTO movement_catalogue(movement_key,revision,active,display_name,body_region,equipment_type,load_basis,review_status,default_period_option_id) VALUES('synthetic-press','test',true,'Synthetic press','CHEST','MACHINE','MACHINE_DISPLAY','REVIEWED',1)");
 }
 String period(String details){return "INSERT INTO exercise_record(user_id,created_at,recorded_at,recorded_date,client_entry_id,source,exercise_type,duration_minutes,data_quality,estimate_status,exercise_option_id,option_name_snapshot,period_kind,equipment_details) VALUES(1,now(),now(),'2026-10-17',gen_random_uuid(),'MANUAL','Synthetic strength',50,'CURRENT','NO_APPLICABLE_REFERENCE',1,'Synthetic strength','STRENGTH','"+details+"')";}
 @Test void legacySnapshotIsUnchangedAndMovementsHaveNoEnergyOrDurationColumns()throws Exception{
  try(var db=database();var c=db.connect();var s=c.createStatement()){
   try(var r=s.executeQuery("SELECT estimated_kj,workout_id,exercise_option_id,source FROM exercise_record WHERE id=1")){assertTrue(r.next());assertEquals("123.456",r.getBigDecimal(1).toPlainString());assertNull(r.getObject(2));assertNull(r.getObject(3));assertEquals("PHOTO",r.getString(4));}
   try(var r=s.executeQuery("SELECT count(*) FROM information_schema.columns WHERE table_name='workout_movement_log' AND column_name IN ('duration_minutes','estimated_kj','met_value')")){assertTrue(r.next());assertEquals(0,r.getInt(1));}
  }
 }
 @Test void nullEnergyNeedsReviewedUnavailableOptionAndValidDetails()throws Exception{
  try(var db=database();var c=db.connect();var s=c.createStatement()){
   options(s);s.execute(period("{}"));
   assertEquals("23514",assertThrows(SQLException.class,()->s.execute(period("{\"met\":10}"))).getSQLState());
   assertEquals("23514",assertThrows(SQLException.class,()->s.execute(period("{\"speedKmh\":\"5\"}"))).getSQLState());
   assertEquals("23514",assertThrows(SQLException.class,()->s.execute("UPDATE exercise_record SET exercise_option_id=NULL WHERE id=2")).getSQLState());
   assertEquals("23514",assertThrows(SQLException.class,()->s.execute("UPDATE exercise_record SET met_used=5 WHERE id=2")).getSQLState());
   assertEquals("23514",assertThrows(SQLException.class,()->s.execute("UPDATE exercise_record SET period_kind='ACTIVITY' WHERE id=2")).getSQLState());
   // General mapping cannot be rewritten after publication; old snapshots retain original option identity.
   assertEquals("23514",assertThrows(SQLException.class,()->s.execute("UPDATE exercise_option SET mapping_kind='SPECIFIC_REFERENCE',reference_id=1 WHERE id=1")).getSQLState());
  }
 }
 @Test void optionalMovementValuesCanBeNullOrPartiallyFilledButNotInvalid()throws Exception{
  try(var db=database();var c=db.connect();var s=c.createStatement()){
   options(s);s.execute(period("{}"));
   s.execute("INSERT INTO workout_movement_log(exercise_record_id,movement_catalogue_id,position,name_snapshot,load_basis_snapshot) VALUES(2,1,0,'Synthetic press','MACHINE_DISPLAY')");
   s.execute("UPDATE workout_movement_log SET set_count=3,load_kg=0 WHERE id=1");
   for(String patch:new String[]{"set_count=0","repetitions=1001","load_kg=-1","load_kg=1000.01"})assertEquals("23514",assertThrows(SQLException.class,()->s.execute("UPDATE workout_movement_log SET "+patch+" WHERE id=1")).getSQLState());
   try(var r=s.executeQuery("SELECT duration_minutes,(SELECT count(*) FROM workout_movement_log) FROM exercise_record WHERE id=2")){assertTrue(r.next());assertEquals("50.00",r.getBigDecimal(1).toPlainString());assertEquals(1,r.getInt(2));}
  }
 }
 @Test void workoutPeriodCrossOwnerAssociationFailsAtomically()throws Exception{
  try(var db=database();var c=db.connect();var s=c.createStatement()){
   options(s);s.execute("INSERT INTO workout_record(user_id,client_entry_id,title,recorded_date,source) VALUES(2,gen_random_uuid(),'Synthetic workout','2026-10-17','MANUAL')");
   c.setAutoCommit(false);s.execute(period("{}"));s.execute("UPDATE exercise_record SET workout_id=1,period_position=0 WHERE id=2");
   assertEquals("23503",assertThrows(SQLException.class,c::commit).getSQLState());c.rollback();
  }
 }
}
