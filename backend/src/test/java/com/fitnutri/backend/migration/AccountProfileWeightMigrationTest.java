package com.fitnutri.backend.migration;

import com.fitnutri.backend.support.TestDatabase;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.flywaydb.core.api.FlywayException;
import static org.junit.jupiter.api.Assertions.*;

class AccountProfileWeightMigrationTest {
    private static final String USER="INSERT INTO app_user(created_at,display_name,email,enabled,password_hash,role) VALUES('2026-01-01T00:00:00Z','Synthetic legacy','legacy@example.test',true,'unchanged-synthetic-hash','ADMIN')";
    @Test void legacyIdentityConsentWeightsAndExplicitGoalArePreserved() throws Exception {
        try(TestDatabase db=TestDatabase.create()) {
            db.flywayAtVersion("1").migrate();
            try(var c=db.connect();var s=c.createStatement()) {
                s.execute(USER);
                s.execute("INSERT INTO user_profile(user_id,ai_qa_eligible,onboarding_completed,health_safety_answer,health_safety_consent,energy_goal_kcal) VALUES(1,true,true,'NOT_SURE',true,2000)");
                s.execute("INSERT INTO weight_entry(created_at,entry_date,weight_kg,user_id) VALUES('2026-01-01T00:00:00Z','2026-01-01',70.12,1)");
            }
            assertEquals(1,db.flywayAtVersion("2").migrate().migrationsExecuted);
            try(var c=db.connect();var s=c.createStatement()) {
                var rows=s.executeQuery("SELECT u.role,u.password_hash,u.version,p.adult_confirmed,p.health_safety_answer,p.health_safety_consent,p.daily_target_kj,p.qa_consent_updated_at,w.weight_kg FROM app_user u JOIN user_profile p ON p.user_id=u.id JOIN weight_entry w ON w.user_id=u.id");
                assertTrue(rows.next());assertEquals("ADMIN",rows.getString(1));assertEquals("unchanged-synthetic-hash",rows.getString(2));assertEquals(0,rows.getLong(3));
                assertFalse(rows.getBoolean(4));assertEquals("YES_OR_UNSURE",rows.getString(5));assertTrue(rows.getBoolean(6));
                assertEquals(new java.math.BigDecimal("8368.000"),rows.getBigDecimal(7));assertNull(rows.getObject(8));assertEquals(new java.math.BigDecimal("70.12"),rows.getBigDecimal(9));assertFalse(rows.next());
            }
        }
    }
    @Test void canonicalEmailCollisionStopsWithoutMergingAccounts() throws Exception {
        try(TestDatabase db=TestDatabase.create()) {
            db.flywayAtVersion("1").migrate();
            try(var c=db.connect();var s=c.createStatement()) {s.execute(USER);s.execute("INSERT INTO app_user(created_at,display_name,email,enabled,password_hash,role) VALUES(now(),'Synthetic2','LEGACY@example.test',true,'unchanged2','USER')");}
            assertThrows(FlywayException.class,()->db.flywayAtVersion("2").migrate());
            try(var c=db.connect();var s=c.createStatement()) {var rows=s.executeQuery("SELECT count(*) FROM app_user");assertTrue(rows.next());assertEquals(2,rows.getInt(1));}
        }
    }
    @Test void duplicateWeightDatesStopWithoutDeletingOrAveraging() throws Exception {
        try(TestDatabase db=TestDatabase.create()) {
            db.flywayAtVersion("1").migrate();
            try(var c=db.connect();var s=c.createStatement()) {s.execute(USER);s.execute("INSERT INTO weight_entry(created_at,entry_date,weight_kg,user_id) VALUES(now(),'2026-01-01',70,1),(now(),'2026-01-01',80,1)");}
            assertThrows(FlywayException.class,()->db.flywayAtVersion("2").migrate());
            try(var c=db.connect();var s=c.createStatement()) {
                var rows=s.executeQuery("SELECT count(*),sum(weight_kg) FROM weight_entry");assertTrue(rows.next());assertEquals(2,rows.getInt(1));assertEquals(150,rows.getDouble(2));
                rows=s.executeQuery("SELECT count(*) FROM information_schema.columns WHERE table_name='weight_entry' AND column_name='version'");assertTrue(rows.next());assertEquals(0,rows.getInt(1));
            }
        }
    }
    @ParameterizedTest
    @ValueSource(strings={"70.125","NaN","Infinity","-Infinity","19.99","400.01"})
    void invalidLegacyWeightCannotBeSilentlyRoundedOrReplaced(String value) throws Exception {
        try(TestDatabase db=TestDatabase.create()) {
            db.flywayAtVersion("1").migrate();
            try(var c=db.connect();var s=c.createStatement()) {s.execute(USER);s.execute("INSERT INTO weight_entry(created_at,entry_date,weight_kg,user_id) VALUES(now(),'2026-01-01','"+value+"',1)");}
            assertThrows(FlywayException.class,()->db.flywayAtVersion("2").migrate());
            try(var c=db.connect();var s=c.createStatement();var rows=s.executeQuery("SELECT weight_kg FROM weight_entry")) {assertTrue(rows.next());assertEquals(Double.parseDouble(value),rows.getDouble(1));}
        }
    }
    @Test void newSchemaEnforcesCanonicalEmailAndDatedWeightUniqueness() throws Exception {
        try(TestDatabase db=TestDatabase.create()) {
            db.flywayAtVersion("2").migrate();
            try(var c=db.connect();var s=c.createStatement()) {
                s.execute(USER);
                var error=assertThrows(java.sql.SQLException.class,()->s.execute("INSERT INTO app_user(created_at,updated_at,display_name,email,enabled,password_hash,role) VALUES(now(),now(),'Synthetic2','LEGACY@example.test',true,'synthetic','USER')"));assertEquals("23505",error.getSQLState());
                s.execute("INSERT INTO weight_entry(created_at,updated_at,entry_date,weight_kg,user_id) VALUES(now(),now(),'2026-01-01',70,1)");
                error=assertThrows(java.sql.SQLException.class,()->s.execute("INSERT INTO weight_entry(created_at,updated_at,entry_date,weight_kg,user_id) VALUES(now(),now(),'2026-01-01',80,1)"));assertEquals("23505",error.getSQLState());
            }
        }
    }

    @ParameterizedTest
    @CsvSource({"YES,YES_OR_UNSURE","PREFER_NOT_TO_SAY,PREFER_NOT_TO_ANSWER","NO,NO"})
    void legacySafetyChoicesMapWithoutInventingConsentOrAdultConfirmation(String before,String after) throws Exception {
        try(TestDatabase db=TestDatabase.create()) {
            db.flywayAtVersion("1").migrate();
            try(var c=db.connect();var s=c.createStatement()) {
                s.execute(USER);
                try(var insert=c.prepareStatement("INSERT INTO user_profile(user_id,ai_qa_eligible,onboarding_completed,health_safety_answer) VALUES(1,false,true,?)")) {insert.setString(1,before);insert.executeUpdate();}
            }
            db.flywayAtVersion("2").migrate();
            try(var c=db.connect();var s=c.createStatement();var rows=s.executeQuery("SELECT health_safety_answer,health_safety_consent,adult_confirmed,qa_consent_updated_at,health_safety_updated_at FROM user_profile")) {
                assertTrue(rows.next());assertEquals(after,rows.getString(1));assertFalse(rows.getBoolean(2));assertFalse(rows.getBoolean(3));assertNull(rows.getObject(4));assertNull(rows.getObject(5));
            }
        }
    }
}
