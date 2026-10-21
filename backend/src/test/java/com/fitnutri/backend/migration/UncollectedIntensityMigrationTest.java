package com.fitnutri.backend.migration;
import com.fitnutri.backend.support.TestDatabase;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class UncollectedIntensityMigrationTest {
    @Test void oldObservedIntensityIsPreservedAndNewUncollectedValueCanBeNull() throws Exception {
        try(var db=TestDatabase.create()){db.flywayAtVersion("7").migrate();try(var c=db.connect();var s=c.createStatement()){
            s.execute("INSERT INTO app_user(id,email,password_hash,display_name,enabled,role,created_at) VALUES(1,'intensity@example.test','synthetic-only','Synthetic',true,'USER',now())");s.execute("INSERT INTO exercise_record(user_id,created_at,recorded_at,recorded_date,client_entry_id,source,exercise_type,duration_minutes,intensity) VALUES(1,now(),now(),'2020-01-01',gen_random_uuid(),'MANUAL','Synthetic original',10,'LOW')");}
            assertEquals(1,db.flywayAtVersion("8").migrate().migrationsExecuted);assertEquals(0,db.flywayAtVersion("8").migrate().migrationsExecuted);
            try(var c=db.connect();var s=c.createStatement()){try(var r=s.executeQuery("SELECT intensity,duration_minutes FROM exercise_record")){r.next();assertEquals("LOW",r.getString(1));assertEquals(new java.math.BigDecimal("10.00"),r.getBigDecimal(2));}
                s.execute("INSERT INTO exercise_record(user_id,created_at,recorded_at,recorded_date,client_entry_id,source,exercise_type,exercise_type_id,duration_minutes,intensity,data_quality,estimate_status) SELECT 1,now(),now(),'2020-01-02',gen_random_uuid(),'MANUAL','Synthetic nullable shape',id,10,NULL,'CURRENT','MISSING_WEIGHT' FROM exercise_type LIMIT 1");try(var r=s.executeQuery("SELECT intensity FROM exercise_record WHERE recorded_date='2020-01-02'")){assertTrue(r.next());assertNull(r.getString(1));}
                assertThrows(java.sql.SQLException.class,()->s.execute("UPDATE exercise_record SET intensity='INVENTED'"));try(var r=s.executeQuery("SELECT count(*) FROM exercise_record")){r.next();assertEquals(2,r.getInt(1));}
            }
        }
    }
}
