package com.fitnutri.backend.migration;
import com.fitnutri.backend.common.ControlledMigration;
import com.fitnutri.backend.support.TestDatabase;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class ControlledMigrationTest {
    @Test void wrongIdentityAndUnadoptedNonemptySchemaAreRejectedBeforeMigration() throws Exception {
        try(var db=TestDatabase.create()) {
            assertThrows(IllegalStateException.class,()->ControlledMigration.migrate(db.url(),db.username(),db.password(),"wrong_database","UNCONFIRMED",false));
            try(var c=db.connect();var s=c.createStatement()){s.execute("CREATE TABLE synthetic_unadopted(id bigint)");}
            assertThrows(IllegalStateException.class,()->ControlledMigration.migrate(db.url(),db.username(),db.password(),cname(db),"UNCONFIRMED",false));
            try(var c=db.connect();var s=c.createStatement();var r=s.executeQuery("SELECT to_regclass('public.flyway_schema_history')")){r.next();assertNull(r.getString(1));}
        }
    }
    @Test void explicitOwnedEmptyTargetCanMigrateAndRerunWithoutSeedingData() throws Exception {
        try(var db=TestDatabase.create()) {
            ControlledMigration.migrate(db.url(),db.username(),db.password(),cname(db),"UNCONFIRMED",false);ControlledMigration.migrate(db.url(),db.username(),db.password(),cname(db),"UNCONFIRMED",false);
            try(var c=db.connect();var s=c.createStatement();var r=s.executeQuery("SELECT count(*),(SELECT count(*) FROM food_reference) FROM flyway_schema_history WHERE success AND version IS NOT NULL")){r.next();assertEquals(com.fitnutri.backend.common.SchemaReadinessGate.APPROVED_VERSION,r.getInt(1));assertEquals(0,r.getInt(2));}
        }
    }
    private String cname(TestDatabase db) throws Exception {try(var c=db.connect()){return c.getCatalog();}}
}
