package com.fitnutri.backend.migration;

import com.fitnutri.backend.support.TestDatabase;
import java.sql.*;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class RecordSnapshotMigrationTest {
    private static final String USER="INSERT INTO app_user(created_at,display_name,email,enabled,password_hash,role) VALUES(now(),'Synthetic','synthetic@example.test',true,'test-only-hash','USER')";
    private static final String FOOD="INSERT INTO food_record(user_id,created_at,recorded_at,source,food_name,quantity_grams,meal_type,protein_grams) VALUES(1,now(),'2020-01-01T23:30:00Z','PHOTO','Synthetic legacy',123.12345,'LUNCH',2.123456)";
    private static final String PROOF="'{\"records\":[{\"id\":\"1\",\"clientEntryId\":\"11111111-1111-4111-8111-111111111111\",\"recordDate\":\"2020-01-01\",\"versionAtCreation\":0}]}'::jsonb";
    private TestDatabase migrated() throws Exception {var db=TestDatabase.create();try{db.flywayAtVersion("4").migrate();try(var c=db.connect();var s=c.createStatement()){s.execute(USER);}return db;}catch(Exception e){db.close();throw e;}}
    @Test void originalInputsRemainExactAndNoDateOrCalculationEvidenceIsInvented() throws Exception {
        try(var db=TestDatabase.create()) {
            db.flywayAtVersion("3").migrate();try(var c=db.connect();var s=c.createStatement()){s.execute(USER);s.execute(FOOD);s.execute("INSERT INTO exercise_record(user_id,created_at,recorded_at,source,exercise_type,duration_minutes,intensity,estimated_calories) VALUES(1,now(),now(),'MANUAL','Legacy walk',30,'LOW',123.5)");}
            assertEquals(1,db.flywayAtVersion("4").migrate().migrationsExecuted);
            try(var c=db.connect();var s=c.createStatement()) {
                try(var r=s.executeQuery("SELECT quantity_grams_legacy,protein_grams_legacy,quantity_grams,recorded_date,client_entry_id,food_reference_id,data_quality,source,version FROM food_record")){r.next();assertEquals(123.12345,r.getDouble(1));assertEquals(2.123456,r.getDouble(2));for(int i=3;i<=6;i++) assertNull(r.getObject(i));assertEquals("LEGACY_UNVERIFIED",r.getString(7));assertEquals("PHOTO",r.getString(8));assertEquals(0,r.getLong(9));}
                try(var r=s.executeQuery("SELECT duration_minutes,estimated_calories,weight_kg_used,weight_date_used,met_used,formula_version,estimate_status FROM exercise_record")){r.next();assertEquals(new java.math.BigDecimal("30.00"),r.getBigDecimal(1));assertEquals(123.5,r.getDouble(2));for(int i=3;i<=6;i++) assertNull(r.getObject(i));assertEquals("LEGACY_UNVERIFIED",r.getString(7));}
            }
        }
    }
    @Test void softDeletionNeverReleasesClientIdentityAndCurrentSnapshotsNeedReferences() throws Exception {
        try(var db=migrated();var c=db.connect();var s=c.createStatement()) {
            String row="INSERT INTO food_record(user_id,created_at,recorded_at,source,food_name,quantity_grams_legacy,meal_type,client_entry_id) VALUES(1,now(),now(),'MANUAL','Synthetic',100,'LUNCH','11111111-1111-4111-8111-111111111111')";
            s.execute("INSERT INTO food_reference(dataset_id,public_food_key,food_name,display_name) VALUES(1,'synthetic-null-check','Synthetic','Synthetic')");
            assertEquals("23514",assertThrows(SQLException.class,()->s.execute("INSERT INTO food_record(user_id,created_at,recorded_at,source,food_name,food_reference_id,quantity_grams,meal_type,data_quality,recorded_date,client_entry_id) VALUES(1,now(),now(),'IMAGE','Synthetic',1,NULL,'LUNCH','CURRENT','2020-01-01',gen_random_uuid())")).getSQLState());
            s.execute(row);s.execute("UPDATE food_record SET deleted_at=now()");assertEquals("23505",assertThrows(SQLException.class,()->s.execute(row)).getSQLState());
            assertEquals("23514",assertThrows(SQLException.class,()->s.execute("INSERT INTO food_record(user_id,created_at,recorded_at,source,food_name,quantity_grams,meal_type,data_quality,recorded_date,client_entry_id) VALUES(1,now(),now(),'IMAGE','Synthetic',100,'LUNCH','CURRENT','2020-01-01',gen_random_uuid())")).getSQLState());
        }
    }
    private String receipt(String uuid,String result,String committed) {return "INSERT INTO submission_receipt(user_id,submission_id,operation,request_sha256,result_json,committed_at) VALUES(1,'"+uuid+"','FOOD_BATCH',repeat('a',64),"+result+","+committed+")";}
    @Test void minimalReceiptProofIsUniqueImmutableAndExcludesHealthData() throws Exception {
        try(var db=migrated();var c=db.connect();var s=c.createStatement()) {
            String id=UUID.randomUUID().toString();s.execute(receipt(id,PROOF,"now()"));
            assertEquals("23505",assertThrows(SQLException.class,()->s.execute(receipt(id,PROOF,"now()"))).getSQLState());
            assertEquals("23514",assertThrows(SQLException.class,()->s.execute("UPDATE submission_receipt SET request_sha256=repeat('b',64)")).getSQLState());
            assertEquals("23514",assertThrows(SQLException.class,()->s.execute("DELETE FROM submission_receipt")).getSQLState());
            assertEquals("23514",assertThrows(SQLException.class,()->s.execute(receipt(UUID.randomUUID().toString(),"'{\"healthSafetyAnswer\":\"NO\"}'::jsonb","now()"))).getSQLState());
            assertEquals("23514",assertThrows(SQLException.class,()->s.execute(receipt(UUID.randomUUID().toString(),"'{}'::jsonb","now()"))).getSQLState());
        }
    }
    @Test void unfinishedReceiptAndRecordCannotCommitAndBothRollback() throws Exception {
        try(var db=migrated();var c=db.connect()) {
            c.setAutoCommit(false);try(var s=c.createStatement()){s.execute("INSERT INTO food_record(user_id,created_at,recorded_at,source,food_name,quantity_grams_legacy,meal_type) VALUES(1,now(),now(),'MANUAL','Synthetic',100,'LUNCH')");s.execute(receipt(UUID.randomUUID().toString(),"NULL","NULL"));}
            assertEquals("23514",assertThrows(SQLException.class,c::commit).getSQLState());c.rollback();
            try(var verify=db.connect();var s=verify.createStatement();var r=s.executeQuery("SELECT (SELECT count(*) FROM food_record),(SELECT count(*) FROM submission_receipt)")){r.next();assertEquals(0,r.getInt(1));assertEquals(0,r.getInt(2));}
        }
    }
    @Test void inFlightReceiptIsInvisibleUntilItsFinalProofCommits() throws Exception {
        try(var db=migrated();var write=db.connect();var read=db.connect()) {
            write.setAutoCommit(false);String id=UUID.randomUUID().toString();try(var s=write.createStatement()){s.execute(receipt(id,"NULL","NULL"));}
            try(var s=read.createStatement();var r=s.executeQuery("SELECT count(*) FROM submission_receipt")){r.next();assertEquals(0,r.getInt(1));}
            try(var s=write.createStatement()){s.execute("UPDATE submission_receipt SET result_json="+PROOF+",committed_at=now()");}write.commit();
            try(var s=read.createStatement();var r=s.executeQuery("SELECT result_json IS NOT NULL,committed_at IS NOT NULL FROM submission_receipt")){assertTrue(r.next());assertTrue(r.getBoolean(1));assertTrue(r.getBoolean(2));}
        }
    }
    @Test void concurrentReceiptIdentityHasOneWinnerAndOneConstraintFailure() throws Exception {
        try(var db=migrated();var pool=Executors.newFixedThreadPool(2)) {
            var start=new CountDownLatch(1);String id=UUID.randomUUID().toString();Callable<String> attempt=()->{try(var c=db.connect();var s=c.createStatement()){start.await();try{s.execute(receipt(id,PROOF,"now()"));return "committed";}catch(SQLException e){return e.getSQLState();}}};
            var a=pool.submit(attempt);var b=pool.submit(attempt);start.countDown();var outcomes=new ArrayList<>(List.of(a.get(15,TimeUnit.SECONDS),b.get(15,TimeUnit.SECONDS)));Collections.sort(outcomes);assertEquals(List.of("23505","committed"),outcomes);
            try(var c=db.connect();var s=c.createStatement();var r=s.executeQuery("SELECT count(*) FROM submission_receipt")){r.next();assertEquals(1,r.getInt(1));}
        }
    }
}
