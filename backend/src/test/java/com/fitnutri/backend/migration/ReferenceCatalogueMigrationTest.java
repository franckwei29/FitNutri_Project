package com.fitnutri.backend.migration;

import com.fitnutri.backend.support.TestDatabase;
import java.math.BigDecimal;
import java.sql.SQLException;
import org.flywaydb.core.api.FlywayException;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ReferenceCatalogueMigrationTest {
    private static final String LEGACY="INSERT INTO food_reference(public_food_key,food_name,energy_kcal,proteing,fatg,carbsg,fiberg,sugarg,sodium_mg) VALUES ('legacy-key','Synthetic legacy food',100,2.5,3,10,1.5,2,33)";
    @Test void legacyKeyNutrientsAndExtensionsSurviveSurrogateIdentityConversion() throws Exception {
        try(var db=TestDatabase.create()) {
            db.flywayAtVersion("2").migrate();try(var c=db.connect();var s=c.createStatement()){s.execute(LEGACY);}
            assertEquals(1,db.flywayAtVersion("3").migrate().migrationsExecuted);
            try(var c=db.connect();var s=c.createStatement();var r=s.executeQuery("SELECT r.id,r.public_food_key,r.fiberg,r.sugarg,r.sodium_mg,r.energy_kj_100g,r.protein_g_100g,r.is_active,d.verified,d.source_url,d.content_sha256 FROM food_reference r JOIN food_dataset d ON d.id=r.dataset_id")) {
                assertTrue(r.next());assertTrue(r.getLong(1)>0);assertEquals("legacy-key",r.getString(2));assertEquals(1.5,r.getDouble(3));assertEquals(2,r.getDouble(4));assertEquals(33,r.getDouble(5));assertEquals(new BigDecimal("418.400"),r.getBigDecimal(6));assertEquals(new BigDecimal("2.500"),r.getBigDecimal(7));assertFalse(r.getBoolean(8));assertFalse(r.getBoolean(9));assertNull(r.getString(10));assertNull(r.getString(11));assertFalse(r.next());
            }
        }
    }
    @Test void unknownIncomingForeignKeyStopsBeforeAnySchemaOrDataChange() throws Exception {
        try(var db=TestDatabase.create()) {
            db.flywayAtVersion("2").migrate();try(var c=db.connect();var s=c.createStatement()){s.execute(LEGACY);s.execute("CREATE TABLE synthetic_external_reference (key varchar(255) REFERENCES food_reference(public_food_key))");s.execute("INSERT INTO synthetic_external_reference VALUES ('legacy-key')");}
            assertThrows(FlywayException.class,()->db.flywayAtVersion("3").migrate());
            try(var c=db.connect();var s=c.createStatement();var r=s.executeQuery("SELECT count(*),(SELECT to_regclass('food_dataset')) FROM synthetic_external_reference")){r.next();assertEquals(1,r.getInt(1));assertNull(r.getString(2));}
        }
    }
    private void dataset(java.sql.Statement s,String revision) throws Exception {s.execute("INSERT INTO food_dataset(name,release_name,revision,nutrient_policy,verified) VALUES ('SYNTHETIC','test-only','"+revision+"','TEST_ONLY',false)");}
    @Test void sameFoodKeyCoexistsAcrossVersionsButOnlyOneActiveAndNoDuplicateDatasetKey() throws Exception {
        try(var db=TestDatabase.create()) {
            db.flywayAtVersion("3").migrate();try(var c=db.connect();var s=c.createStatement()) {
                dataset(s,"one");dataset(s,"two");
                s.execute("INSERT INTO food_reference(dataset_id,public_food_key,food_name,official_name,display_name,is_active) SELECT id,'same-key','Synthetic','Synthetic','Synthetic',true FROM food_dataset WHERE name='SYNTHETIC' AND revision='one'");
                s.execute("INSERT INTO food_reference(dataset_id,public_food_key,food_name,official_name,display_name,is_active) SELECT id,'same-key','Synthetic2','Synthetic2','Synthetic2',false FROM food_dataset WHERE name='SYNTHETIC' AND revision='two'");
                assertEquals("23505",assertThrows(SQLException.class,()->s.execute("UPDATE food_reference SET is_active=true WHERE food_name='Synthetic2'")).getSQLState());
                assertEquals("23505",assertThrows(SQLException.class,()->s.execute("INSERT INTO food_reference(dataset_id,public_food_key,food_name,official_name,display_name) SELECT id,'same-key','Duplicate','Duplicate','Duplicate' FROM food_dataset WHERE name='SYNTHETIC' AND revision='one'")).getSQLState());
                s.execute("UPDATE food_reference SET is_active=false WHERE food_name='Synthetic'");s.execute("UPDATE food_reference SET is_active=true WHERE food_name='Synthetic2'");
                try(var r=s.executeQuery("SELECT count(*),count(DISTINCT id) FROM food_reference WHERE public_food_key='same-key'")){r.next();assertEquals(2,r.getInt(1));assertEquals(2,r.getInt(2));}
            }
        }
    }
    @Test void referenceAndDatasetHistoryCannotBeRewrittenOrDeleted() throws Exception {
        try(var db=TestDatabase.create()) {
            db.flywayAtVersion("2").migrate();try(var c=db.connect();var s=c.createStatement()){s.execute(LEGACY);}db.flywayAtVersion("3").migrate();
            try(var c=db.connect();var s=c.createStatement()) {
                assertEquals("23514",assertThrows(SQLException.class,()->s.execute("UPDATE food_reference SET public_food_key='changed'")).getSQLState());
                assertEquals("23514",assertThrows(SQLException.class,()->s.execute("UPDATE food_reference SET energy_kj_100g=999")).getSQLState());
                assertEquals("23514",assertThrows(SQLException.class,()->s.execute("DELETE FROM food_reference")).getSQLState());
                assertEquals("23514",assertThrows(SQLException.class,()->s.execute("UPDATE food_dataset SET verified=true")).getSQLState());
            }
        }
    }
    @Test void activityVersionsActiveKeysSourceFieldsAndFinitePositiveMetAreConstrained() throws Exception {
        try(var db=TestDatabase.create()) {
            db.flywayAtVersion("3").migrate();try(var c=db.connect();var s=c.createStatement()) {
                s.execute("INSERT INTO activity_dataset(name,release_name,revision,verified) VALUES ('SYNTHETIC','test-only','one',false),('SYNTHETIC','test-only','two',false)");
                s.execute("INSERT INTO exercise_type(dataset_id,activity_key,display_name,source_activity_code,source_description,met_value,is_active) SELECT id,'walk','Synthetic walk','test-code','Synthetic source',3.50,true FROM activity_dataset WHERE revision='one'");
                assertEquals("23505",assertThrows(SQLException.class,()->s.execute("INSERT INTO exercise_type(dataset_id,activity_key,display_name,met_value,is_active) SELECT id,'walk','Other version',4,true FROM activity_dataset WHERE revision='two'")).getSQLState());
                assertEquals("23514",assertThrows(SQLException.class,()->s.execute("INSERT INTO exercise_type(dataset_id,activity_key,display_name,met_value) SELECT id,'invalid','Invalid','NaN' FROM activity_dataset LIMIT 1")).getSQLState());
                try(var r=s.executeQuery("SELECT source_activity_code,source_description,met_value FROM exercise_type WHERE activity_key='walk'")){r.next();assertEquals("test-code",r.getString(1));assertEquals("Synthetic source",r.getString(2));assertEquals(new BigDecimal("3.50"),r.getBigDecimal(3));}
            }
        }
    }
    @Test void verifiedDatasetsRequireRealProvenanceAndNeverZeroFingerprint() throws Exception {
        try(var db=TestDatabase.create()) {
            db.flywayAtVersion("3").migrate();try(var c=db.connect();var s=c.createStatement()) {
                assertEquals("23514",assertThrows(SQLException.class,()->s.execute("INSERT INTO food_dataset(name,release_name,revision,nutrient_policy,verified) VALUES ('Synthetic','test','one','TEST',true)")).getSQLState());
                assertEquals("23514",assertThrows(SQLException.class,()->s.execute("INSERT INTO food_dataset(name,release_name,revision,nutrient_policy,verified,content_sha256,source_url) VALUES ('Synthetic','test','one','TEST',true,repeat('0',64),'https://example.test/synthetic')")).getSQLState());
                assertEquals("23514",assertThrows(SQLException.class,()->s.execute("INSERT INTO activity_dataset(name,release_name,revision,verified,content_sha256,source_url) VALUES ('Synthetic','test','one',true,repeat('a',64),'https://example.test/synthetic')")).getSQLState());
            }
        }
    }
}
