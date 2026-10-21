package com.fitnutri.backend.migration;

import com.fitnutri.backend.support.TestDatabase;
import java.math.BigDecimal;
import java.util.Map;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.FlywayException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.junit.jupiter.api.Assertions.*;

class LegacyBackfillMigrationTest {
    private Flyway migrate(TestDatabase db,String zone,boolean approved) {return Flyway.configure().dataSource(db.url(),db.username(),db.password()).cleanDisabled(true).baselineOnMigrate(false).target("7").placeholders(Map.of("legacy_zone",zone,"legacy_zone_approved",Boolean.toString(approved))).load();}
    private void seed(TestDatabase db) throws Exception {
        db.flywayAtVersion("4").migrate();try(var c=db.connect();var s=c.createStatement()) {
            s.execute("INSERT INTO app_user(created_at,display_name,email,enabled,password_hash,role) VALUES(now(),'Synthetic','backfill@example.test',true,'test-only-hash','USER')");
            s.execute("INSERT INTO food_record(user_id,created_at,recorded_at,source,food_name,quantity_grams_legacy,meal_type,calories,protein_grams_legacy) VALUES(1,'2026-01-01T00:00:00Z','2020-01-01T23:30:00Z','PHOTO','Synthetic legacy',123.12345,'LUNCH',100.12345,2.123456)");
            s.execute("INSERT INTO exercise_record(user_id,created_at,recorded_at,source,exercise_type,duration_minutes,intensity,estimated_calories) VALUES(1,now(),'2020-01-01T23:30:00Z','MANUAL','Synthetic walk',30,'LOW',123.5)");
        }
    }
    @Test void emptyDatabaseNeedsNoGuessedZoneAndRerunMakesNoChanges() throws Exception {
        try(var db=TestDatabase.create()) {
            assertEquals(7,migrate(db,"UNCONFIRMED",false).migrate().migrationsExecuted);assertEquals(0,migrate(db,"UNCONFIRMED",false).migrate().migrationsExecuted);
            try(var c=db.connect();var s=c.createStatement();var r=s.executeQuery("SELECT count(*) FROM information_schema.tables WHERE table_schema='public' AND table_name<>'flyway_schema_history'")){r.next();assertEquals(10,r.getInt(1));}
        }
    }
    @Test void explicitApprovedZoneConvertsDatesUnitsAndGeneratedIdsWithoutInventingEvidence() throws Exception {
        try(var db=TestDatabase.create()) {seed(db);assertEquals(3,migrate(db,"Australia/Sydney",true).migrate().migrationsExecuted);
            try(var c=db.connect();var s=c.createStatement()) {
                try(var r=s.executeQuery("SELECT recorded_date,quantity_grams,quantity_grams_legacy,energy_kj,protein_grams,client_entry_id,food_reference_id,data_quality,recorded_at,source,calories,protein_grams_legacy FROM food_record")){r.next();assertEquals("2020-01-02",r.getString(1));assertEquals(new BigDecimal("123.123"),r.getBigDecimal(2));assertEquals(123.12345,r.getDouble(3));assertEquals(new BigDecimal("418.917"),r.getBigDecimal(4));assertEquals(new BigDecimal("2.123"),r.getBigDecimal(5));assertNotNull(r.getObject(6));assertNull(r.getObject(7));assertEquals("LEGACY_UNVERIFIED",r.getString(8));assertEquals("2020-01-01T23:30:00Z",r.getTimestamp(9).toInstant().toString());assertEquals("PHOTO",r.getString(10));assertEquals(100.12345,r.getDouble(11));assertEquals(2.123456,r.getDouble(12));}
                try(var r=s.executeQuery("SELECT recorded_date,estimated_kj,weight_kg_used,met_used,formula_version,estimate_status FROM exercise_record")){r.next();assertEquals("2020-01-02",r.getString(1));assertEquals(new BigDecimal("516.724"),r.getBigDecimal(2));for(int i=3;i<=5;i++) assertNull(r.getObject(i));assertEquals("LEGACY_UNVERIFIED",r.getString(6));}
            }
            assertEquals(0,migrate(db,"Australia/Sydney",true).migrate().migrationsExecuted);
        }
    }
    @ParameterizedTest @ValueSource(strings={"UNCONFIRMED","Invalid/Zone",""})
    void invalidOrUnknownLegacyZoneStopsWithoutChangingOriginalRows(String zone) throws Exception {
        try(var db=TestDatabase.create()) {seed(db);assertThrows(FlywayException.class,()->migrate(db,zone,true).migrate());
            try(var c=db.connect();var s=c.createStatement();var r=s.executeQuery("SELECT recorded_date,quantity_grams,quantity_grams_legacy FROM food_record")){r.next();assertNull(r.getObject(1));assertNull(r.getObject(2));assertEquals(123.12345,r.getDouble(3));}
        }
    }
    @Test void validButUnapprovedZoneIsStillRejected() throws Exception {try(var db=TestDatabase.create()){seed(db);assertThrows(FlywayException.class,()->migrate(db,"Australia/Sydney",false).migrate());}}
    @Test void invalidLegacyQuantityStopsRatherThanDeletingOrClampingIt() throws Exception {
        try(var db=TestDatabase.create()) {seed(db);try(var c=db.connect();var s=c.createStatement()){s.execute("UPDATE food_record SET quantity_grams_legacy='NaN'");}
            assertThrows(FlywayException.class,()->migrate(db,"Australia/Sydney",true).migrate());try(var c=db.connect();var s=c.createStatement();var r=s.executeQuery("SELECT quantity_grams_legacy::text,recorded_date FROM food_record")){r.next();assertEquals("NaN",r.getString(1));assertNull(r.getObject(2));}
        }
    }
    @Test void unknownNutrientsRemainNullAndRealZeroRemainsKnownZero() throws Exception {
        try(var db=TestDatabase.create()){seed(db);try(var c=db.connect();var s=c.createStatement()){s.execute("UPDATE food_record SET calories=NULL,protein_grams_legacy=0,carbs_grams_legacy='NaN',fat_grams_legacy=NULL");}
            migrate(db,"Australia/Sydney",true).migrate();try(var c=db.connect();var s=c.createStatement();var r=s.executeQuery("SELECT energy_kj,protein_grams,carbs_grams,fat_grams,carbs_grams_legacy::text FROM food_record")){r.next();assertNull(r.getObject(1));assertEquals(new BigDecimal("0.000"),r.getBigDecimal(2));assertNull(r.getObject(3));assertNull(r.getObject(4));assertEquals("NaN",r.getString(5));}
        }
    }
}
