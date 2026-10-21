package com.fitnutri.backend.common;

import java.sql.DriverManager;
import org.flywaydb.core.Flyway;

/** Standalone maintenance entrypoint. Never bootstraps Hibernate or normal app writers. */
public final class LegacyBaselineAdoption {
    private LegacyBaselineAdoption() {}
    public static void main(String[] args) throws Exception {
        if(!"1".equals(required("FITNUTRI_BASELINE_APPROVED")) || !"1".equals(required("FITNUTRI_BACKUP_VERIFIED")))
            throw new IllegalStateException("Explicit reviewed adoption and verified backup are required.");
        adopt(required("DB_URL"),required("DB_USERNAME"),required("DB_PASSWORD"),required("FITNUTRI_DATABASE_IDENTITY"));
        System.out.println("Reviewed legacy V1 explicitly baselined; no application data changed.");
    }
    public static void adopt(String url,String username,String password,String expectedIdentity) throws Exception {
        try(var connection=DriverManager.getConnection(url,username,password)) {
            connection.setReadOnly(true);
            if(!expectedIdentity.equals(connection.getCatalog())) throw new IllegalStateException("Database identity mismatch; adoption blocked.");
            LegacySchemaVerifier.verify(connection);
            try(var statement=connection.createStatement()) {
                statement.setQueryTimeout(15);
                var rows=statement.executeQuery("SELECT (SELECT count(*) FROM (SELECT lower(btrim(email)) FROM app_user GROUP BY lower(btrim(email)) HAVING count(*)>1) e) + (SELECT count(*) FROM (SELECT user_id,entry_date FROM weight_entry GROUP BY user_id,entry_date HAVING count(*)>1) w)");
                if(!rows.next() || rows.getLong(1)!=0) throw new IllegalStateException("Unresolved account/weight collisions; adoption blocked.");
                rows=statement.executeQuery("SELECT to_regclass('public.flyway_schema_history')");
                if(rows.next() && rows.getString(1)!=null) throw new IllegalStateException("Existing Flyway history; reviewed legacy adoption does not apply.");
            }
        }
        Flyway.configure().dataSource(url,username,password).baselineOnMigrate(false).baselineVersion("1").cleanDisabled(true).load().baseline();
    }
    private static String required(String name) {
        String value=System.getenv(name);
        if(value==null || value.isBlank()) throw new IllegalStateException("Explicit "+name+" is required.");
        return value;
    }
}
