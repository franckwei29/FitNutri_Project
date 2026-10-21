package com.fitnutri.backend;

import com.fitnutri.backend.support.PostgresIntegrationSupport;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import static org.junit.jupiter.api.Assertions.*;

class BackendApplicationTests extends PostgresIntegrationSupport {
    @Autowired private DataSource dataSource;
    @Test void contextUsesRealPostgres16AndVersionedBaseline() throws Exception {
        try(var connection=dataSource.getConnection();var statement=connection.createStatement()) {
            assertEquals("PostgreSQL",connection.getMetaData().getDatabaseProductName());
            assertEquals(16,connection.getMetaData().getDatabaseMajorVersion());
            var rows=statement.executeQuery("SELECT version,success FROM flyway_schema_history WHERE version='1'");
            assertTrue(rows.next());assertEquals("1",rows.getString(1));assertTrue(rows.getBoolean(2));assertFalse(rows.next());
        }
    }
}
