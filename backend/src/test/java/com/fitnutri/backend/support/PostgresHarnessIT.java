package com.fitnutri.backend.support;

import org.junit.jupiter.api.Test;
import java.sql.SQLException;
import static org.junit.jupiter.api.Assertions.*;

/** Failsafe *IT discovery and real PostgreSQL transaction/constraint smoke. */
class PostgresHarnessIT {
    @Test void rollbackAndUniqueConstraintsAreRealPostgresBehaviour() throws Exception {
        try(TestDatabase db=TestDatabase.create()) {
            db.flyway().migrate();
            try(var connection=db.connect();var statement=connection.createStatement()) {
                connection.setAutoCommit(false);
                String insert="INSERT INTO app_user(created_at,display_name,email,enabled,password_hash,role) VALUES(now(),'Synthetic','unique@example.test',true,'synthetic-only','USER')";
                statement.execute(insert);
                SQLException duplicate=assertThrows(SQLException.class,()->statement.execute(insert));
                assertEquals("23505",duplicate.getSQLState());
                connection.rollback();
                var rows=statement.executeQuery("SELECT count(*) FROM app_user");
                assertTrue(rows.next());assertEquals(0,rows.getInt(1));
                connection.rollback();
            }
        }
    }
}
