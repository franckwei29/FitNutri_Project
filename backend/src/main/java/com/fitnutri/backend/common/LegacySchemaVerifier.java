package com.fitnutri.backend.common;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;

/** Explicit legacy adoption gate. Read-only; never creates a Flyway baseline. */
public final class LegacySchemaVerifier {
    private LegacySchemaVerifier() {}
    public static void verify(Connection connection) {
        try (var sql=LegacySchemaVerifier.class.getResourceAsStream("/db/legacy-schema-signature.sql");
             var expected=LegacySchemaVerifier.class.getResourceAsStream("/db/legacy-schema-signature.json");
             var statement=connection.createStatement()) {
            if(sql==null || expected==null) throw new IllegalStateException("Reviewed baseline signature is missing.");
            statement.setQueryTimeout(15);
            var mapper=new ObjectMapper();
            try(var rows=statement.executeQuery(new String(sql.readAllBytes(),StandardCharsets.UTF_8))) {
                if(!rows.next() || !mapper.readTree(expected).equals(mapper.readTree(rows.getString(1))))
                    throw new IllegalStateException("Database schema does not match reviewed V1; adoption blocked.");
            }
        } catch (IllegalStateException e) { throw e; }
        catch (Exception e) { throw new IllegalStateException("Could not verify legacy schema; adoption blocked.",e); }
    }
}
