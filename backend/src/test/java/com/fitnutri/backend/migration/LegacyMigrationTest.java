package com.fitnutri.backend.migration;

import com.fitnutri.backend.support.TestDatabase;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import static org.junit.jupiter.api.Assertions.*;

class LegacyMigrationTest {
    @Test void emptyPostgres16MigratesAndSecondRunDoesNothing() throws Exception {
        try (TestDatabase db=TestDatabase.create()) {
            Flyway flyway=db.flywayAtVersion("1");
            assertEquals(1,flyway.migrate().migrationsExecuted);
            assertEquals(0,flyway.migrate().migrationsExecuted);
            assertEquals(0,flyway.validateWithResult().invalidMigrations.size());
            try (Connection connection=db.connect(); var statement=connection.createStatement()) {
                var tables=statement.executeQuery("SELECT count(*) FROM information_schema.tables WHERE table_schema='public' AND table_name <> 'flyway_schema_history'");
                assertTrue(tables.next()); assertEquals(6,tables.getInt(1));
                var columns=statement.executeQuery("SELECT data_type FROM information_schema.columns WHERE table_name='food_reference' AND column_name='carbsg'");
                assertTrue(columns.next()); assertEquals("double precision",columns.getString(1));
            }
        }
    }
    @Test void nonemptyDatabaseRequiresExplicitReviewedAdoption() throws Exception {
        try (TestDatabase db=TestDatabase.create()) {
            try (var input=getClass().getResourceAsStream("/db/migration/V1__legacy_schema_baseline.sql");var connection=db.connect();var statement=connection.createStatement()) {
                if(input==null) throw new IllegalStateException("Missing V1 fixture");
                statement.execute(new String(input.readAllBytes(),StandardCharsets.UTF_8));
                statement.execute("INSERT INTO app_user(created_at,display_name,email,enabled,password_hash,role) VALUES(now(),'Synthetic legacy','legacy@example.test',true,'synthetic-not-login','USER')");
            }
            assertThrows(org.flywaydb.core.api.FlywayException.class,()->db.flywayAtVersion("1").migrate());
            try(var connection=db.connect()) { com.fitnutri.backend.common.LegacySchemaVerifier.verify(connection); }
            com.fitnutri.backend.common.LegacyBaselineAdoption.adopt(db.url(),db.username(),db.password(),db.url().substring(db.url().lastIndexOf('/')+1));
            Flyway flyway=db.flywayAtVersion("1");
            assertEquals(0,flyway.migrate().migrationsExecuted);
            try(var connection=db.connect();var statement=connection.createStatement();var rows=statement.executeQuery("SELECT email,password_hash FROM app_user")) {
                assertTrue(rows.next());assertEquals("legacy@example.test",rows.getString(1));assertEquals("synthetic-not-login",rows.getString(2));assertFalse(rows.next());
            }
        }
    }
    @Test void mismatchedSchemaIsRejectedWithoutAutomaticBaseline() throws Exception {
        try(TestDatabase db=TestDatabase.create()) {
            try(var connection=db.connect();var statement=connection.createStatement()) {
                statement.execute("CREATE TABLE synthetic_wrong_schema(id bigint)");
                assertThrows(IllegalStateException.class,()->com.fitnutri.backend.common.LegacySchemaVerifier.verify(connection));
                assertThrows(org.flywaydb.core.api.FlywayException.class,()->db.flywayAtVersion("1").migrate());
                var tables=statement.executeQuery("SELECT to_regclass('public.flyway_schema_history')");
                assertTrue(tables.next());assertNull(tables.getString(1));
            }
        }
    }

    @Test void alteredConstraintAndIdentityMismatchCannotBeAdopted() throws Exception {
        try(TestDatabase db=TestDatabase.create()) {
            db.flywayAtVersion("1").migrate();
            assertThrows(IllegalStateException.class,()->com.fitnutri.backend.common.LegacyBaselineAdoption.adopt(db.url(),db.username(),db.password(),"different-database"));
            try(var connection=db.connect();var statement=connection.createStatement()) {
                statement.execute("ALTER TABLE app_user DROP CONSTRAINT app_user_role_check");
                statement.execute("ALTER TABLE app_user ADD CONSTRAINT app_user_role_check CHECK (role IN ('USER','ADMIN','INJECTED'))");
                assertThrows(IllegalStateException.class,()->com.fitnutri.backend.common.LegacySchemaVerifier.verify(connection));
            }
        }
    }
}
