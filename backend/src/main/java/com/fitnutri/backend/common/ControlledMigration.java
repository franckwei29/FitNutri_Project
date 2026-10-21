package com.fitnutri.backend.common;

import java.sql.DriverManager;
import org.flywaydb.core.Flyway;

/** Offline maintenance only: validates target identity and explicit approval before any migration. */
public final class ControlledMigration {
    private ControlledMigration() {}
    public static void main(String[] args) throws Exception {
        if(!"1".equals(required("FITNUTRI_MIGRATION_APPROVED"))||!"1".equals(required("FITNUTRI_BACKUP_VERIFIED"))) throw new IllegalStateException("Reviewed migration and verified backup approval are required.");
        migrate(required("DB_URL"),required("DB_USERNAME"),required("DB_PASSWORD"),required("FITNUTRI_DATABASE_IDENTITY"),System.getenv().getOrDefault("LEGACY_RECORD_ZONE","UNCONFIRMED"),"true".equals(System.getenv("LEGACY_RECORD_ZONE_APPROVED")));
        System.out.println("Controlled migration complete. Run read-only verification before starting the application.");
    }
    public static void migrate(String url,String username,String password,String expectedDatabase,String zone,boolean zoneApproved) throws Exception {
        var policy=new LegacyMigrationPolicy(zone,zoneApproved);
        try(var c=DriverManager.getConnection(url,username,password)) {
            c.setReadOnly(true);
            if(!expectedDatabase.equals(c.getCatalog())) throw new IllegalStateException("Database identity mismatch; migration blocked.");
            try(var s=c.createStatement();var r=s.executeQuery("SELECT to_regclass('public.flyway_schema_history'),(SELECT count(*) FROM information_schema.tables WHERE table_schema='public')")) {
                r.next();if(r.getString(1)==null&&r.getLong(2)>0) throw new IllegalStateException("Existing data requires reviewed explicit baseline adoption first.");
            }
        }
        var config=Flyway.configure().dataSource(url,username,password).baselineOnMigrate(false).cleanDisabled(true);policy.customize(config);config.load().migrate();
    }
    private static String required(String key) {String value=System.getenv(key);if(value==null||value.isBlank()) throw new IllegalStateException("Set "+key+" explicitly.");return value;}
}
