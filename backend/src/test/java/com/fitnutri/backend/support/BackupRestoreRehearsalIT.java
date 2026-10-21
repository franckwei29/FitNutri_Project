package com.fitnutri.backend.support;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Path;
import java.sql.*;
import java.util.*;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** Actual pg_dump/pg_restore, maintenance scripts and reconciliation, only owned UUID databases. */
class BackupRestoreRehearsalIT {
    private record Result(int status,String output) {}
    private Result run(List<String> command) throws Exception {
        var outputFile=java.nio.file.Files.createTempFile("fitnutri-maintenance-output-",".log");
        try {
            var process=new ProcessBuilder(command).redirectErrorStream(true).redirectOutput(outputFile.toFile()).start();
            if(!process.waitFor(30,java.util.concurrent.TimeUnit.SECONDS)) {process.destroyForcibly();process.waitFor(5,java.util.concurrent.TimeUnit.SECONDS);fail("Maintenance process exceeded its test bound.");}
            return new Result(process.exitValue(),java.nio.file.Files.readString(outputFile));
        } finally {java.nio.file.Files.deleteIfExists(outputFile);}
    }
    private Result docker(String... args) throws Exception {var command=new ArrayList<String>();command.add("docker");command.addAll(List.of(args));return run(command);}
    private Result script(String container,TestDatabase db,String database,String script,String... extra) throws Exception {
        var command=new ArrayList<String>(List.of("docker","exec","--env","PGHOST=127.0.0.1","--env","PGPORT=5432","--env","PGUSER="+db.username(),"--env","PGPASSWORD="+db.password(),"--env","PGDATABASE="+database,"--env","FITNUTRI_DATABASE_IDENTITY="+database));
        command.addAll(List.of(extra));command.add(container);command.add("sh");command.add(script);return run(command);
    }
    private void success(Result result) {assertEquals(0,result.status(),result.output());}
    private String database(TestDatabase db) throws Exception {try(var c=db.connect()){return c.getCatalog();}}
    private String targetUrl(TestDatabase db,String name) {return db.url().substring(0,db.url().lastIndexOf('/')+1)+name;}
    private JsonNode report(Result result) throws Exception {success(result);String line=result.output().lines().filter(s->s.startsWith("{")).findFirst().orElseThrow();var json=new ObjectMapper().readTree(line);((com.fasterxml.jackson.databind.node.ObjectNode)json).remove("database");return json;}
    private void applicationProbe(TestDatabase db,String target,boolean compatible) throws Exception {
        Path log=java.nio.file.Files.createTempFile("fitnutri-restored-app-",".log");
        var builder=new ProcessBuilder(Path.of(System.getProperty("java.home"),"bin","java").toString(),"-jar",Path.of("target","backend-0.0.1-SNAPSHOT.jar").toAbsolutePath().toString(),"--server.port=0");
        var env=builder.environment();env.put("SPRING_PROFILES_ACTIVE","local");env.put("SPRING_DATASOURCE_URL",targetUrl(db,target));env.put("SPRING_DATASOURCE_USERNAME",db.username());env.put("SPRING_DATASOURCE_PASSWORD",db.password());env.put("SPRING_FLYWAY_ENABLED","false");env.put("APP_BOOTSTRAP_ADMIN_ENABLED","false");env.put("IMPORT_FOOD_CATALOGUE","false");env.put("IMPORT_ACTIVITY_CATALOGUE","false");
        for(String feature:List.of("IMAGE","TRANSCRIPTION","PARSING","QA"))env.put("AI_"+feature+"_ENABLED","false");
        var process=builder.redirectErrorStream(true).redirectOutput(log.toFile()).start();
        try {
            if(!compatible){assertTrue(process.waitFor(15,java.util.concurrent.TimeUnit.SECONDS),"Incompatible V4 schema must fail startup within the bound.");assertNotEquals(0,process.exitValue());String output=java.nio.file.Files.readString(log);assertTrue(output.contains("Database schema readiness requires the approved V"+com.fitnutri.backend.common.SchemaReadinessGate.APPROVED_VERSION+" migration history"),"Failure must be the explicit read-only schema-version gate.");try(var c=java.sql.DriverManager.getConnection(targetUrl(db,target),db.username(),db.password());var statement=c.createStatement();var r=statement.executeQuery("SELECT max(version::integer) FROM flyway_schema_history WHERE success")){r.next();assertEquals(4,r.getInt(1));}return;}
            int port=0;long deadline=System.nanoTime()+java.time.Duration.ofSeconds(15).toNanos();
            while(System.nanoTime()<deadline&&process.isAlive()){var matcher=java.util.regex.Pattern.compile("Tomcat started on port ([0-9]+)").matcher(java.nio.file.Files.readString(log));if(matcher.find()){port=Integer.parseInt(matcher.group(1));break;}Thread.sleep(100);}
            assertTrue(port>0,"Restored application must actually start.");
            final String base="http://127.0.0.1:"+port;
            try(var client=java.net.http.HttpClient.newBuilder().cookieHandler(new java.net.CookieManager(null,java.net.CookiePolicy.ACCEPT_ALL)).connectTimeout(java.time.Duration.ofSeconds(3)).build()) {
                var csrfResponse=client.send(java.net.http.HttpRequest.newBuilder(java.net.URI.create(base+"/api/auth/csrf")).timeout(java.time.Duration.ofSeconds(5)).GET().build(),java.net.http.HttpResponse.BodyHandlers.ofString());assertEquals(200,csrfResponse.statusCode());String token=new ObjectMapper().readTree(csrfResponse.body()).get("token").asText();
                var login=client.send(java.net.http.HttpRequest.newBuilder(java.net.URI.create(base+"/api/auth/login")).timeout(java.time.Duration.ofSeconds(5)).header("X-CSRF-TOKEN",token).header("Content-Type","application/json").POST(java.net.http.HttpRequest.BodyPublishers.ofString("{\"email\":\"restore@example.test\",\"password\":\"SyntheticRestore123\"}")).build(),java.net.http.HttpResponse.BodyHandlers.ofString());assertEquals(200,login.statusCode());
                var food=client.send(java.net.http.HttpRequest.newBuilder(java.net.URI.create(base+"/api/records/food/1")).timeout(java.time.Duration.ofSeconds(5)).GET().build(),java.net.http.HttpResponse.BodyHandlers.ofString());assertEquals(200,food.statusCode());var f=new ObjectMapper().readTree(food.body());assertEquals("2020-01-02",f.get("recordDate").asText());assertEquals("IMAGE",f.get("source").asText());assertEquals("LEGACY_UNVERIFIED",f.get("dataQuality").asText());assertEquals(418.917,f.at("/nutrients/energyKj").asDouble());assertTrue(f.get("reference").isNull());assertTrue(f.at("/nutrients/fatGrams").isNull());
                var exercise=client.send(java.net.http.HttpRequest.newBuilder(java.net.URI.create(base+"/api/records/exercise/1")).timeout(java.time.Duration.ofSeconds(5)).GET().build(),java.net.http.HttpResponse.BodyHandlers.ofString());assertEquals(200,exercise.statusCode());var e=new ObjectMapper().readTree(exercise.body());assertEquals(516.724,e.get("estimatedKj").asDouble());assertEquals("LEGACY_UNVERIFIED",e.get("estimateStatus").asText());assertTrue(e.get("weightKgUsed").isNull());assertTrue(e.get("formulaVersion").isNull());
                System.out.println("Restored executable application: real login + legacy food/exercise reads PASS; missing provenance remains NULL.");
            }
        } finally {process.destroy();if(!process.waitFor(5,java.util.concurrent.TimeUnit.SECONDS)){process.destroyForcibly();process.waitFor(5,java.util.concurrent.TimeUnit.SECONDS);}java.nio.file.Files.deleteIfExists(log);}
    }
    @Test void dumpRestoreAndApprovedLegacyUpgradeRetainRowsChecksumsAndConstraints() throws Exception {
        String container=System.getenv("TEST_DB_DOCKER_CONTAINER");assertNotNull(container,"Set TEST_DB_DOCKER_CONTAINER to the dedicated test PostgreSQL16 container; no shared fallback.");
        String target="fitnutri_it_"+UUID.randomUUID().toString().replace("-","");assertTrue(target.matches("fitnutri_it_[0-9a-f]{32}"));
        String directory="/tmp/fitnutri-maintenance-"+UUID.randomUUID();
        try(var db=TestDatabase.create()) {
            db.flywayAtVersion("4").migrate();String source=database(db);
            try(var c=db.connect();var s=c.createStatement()) {
                try(var insert=c.prepareStatement("INSERT INTO app_user(created_at,display_name,email,enabled,password_hash,role) VALUES(now(),'Synthetic restore fixture','restore@example.test',true,?,'USER')")){insert.setString(1,new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder().encode("SyntheticRestore123"));insert.executeUpdate();}
                s.execute("INSERT INTO food_record(user_id,created_at,recorded_at,source,food_name,quantity_grams_legacy,meal_type,calories,protein_grams_legacy) VALUES(1,now(),'2020-01-01T23:30:00Z','PHOTO','Synthetic legacy food',123.12345,'LUNCH',100.12345,2.123456)");
                s.execute("INSERT INTO exercise_record(user_id,created_at,recorded_at,source,exercise_type,duration_minutes,intensity,estimated_calories) VALUES(1,now(),'2020-01-01T23:30:00Z','MANUAL','Synthetic walk',30,'LOW',123.5)");
                try(var r=s.executeQuery("SELECT count(*) FROM pg_database WHERE datname='"+target+"'")){r.next();assertEquals(0,r.getInt(1));}
            }
            success(docker("exec",container,"mkdir","-m","700",directory));
            success(docker("cp",Path.of("..","scripts","db").toAbsolutePath().normalize().toString(),container+":"+directory+"/db"));
            String backup=directory+"/before-v5.dump",scripts=directory+"/db/";
            success(script(container,db,source,scripts+"backup.sh","--env","BACKUP_FILE="+backup));
            assertNotEquals(0,script(container,db,source,scripts+"backup.sh","--env","BACKUP_FILE="+backup).status(),"Existing backup must not be overwritten.");
            try {
                assertNotEquals(0,script(container,db,source,scripts+"restore-to-new-db.sh","--env","BACKUP_FILE="+backup,"--env","TARGET_DATABASE="+target).status(),"Restore requires explicit approval.");
                success(script(container,db,source,scripts+"restore-to-new-db.sh","--env","BACKUP_FILE="+backup,"--env","TARGET_DATABASE="+target,"--env","FITNUTRI_RESTORE_APPROVED=1"));
                assertNotEquals(0,script(container,db,source,scripts+"restore-to-new-db.sh","--env","BACKUP_FILE="+backup,"--env","TARGET_DATABASE="+target,"--env","FITNUTRI_RESTORE_APPROVED=1").status(),"Existing target must not be overwritten.");
                // Current binary must fail closed on the V4 restore, without implicit schema writes.
                applicationProbe(db,target,false);
                var policy=Map.of("legacy_zone","Australia/Sydney","legacy_zone_approved","true");
                Flyway.configure().dataSource(db.url(),db.username(),db.password()).cleanDisabled(true).placeholders(policy).load().migrate();
                Flyway.configure().dataSource(targetUrl(db,target),db.username(),db.password()).cleanDisabled(true).placeholders(policy).load().migrate();
                JsonNode original=report(script(container,db,source,scripts+"verify.sh")),restored=report(script(container,db,target,scripts+"verify.sh"));
                assertEquals(original,restored,"Restore must preserve counts, original column checksums, known sums, dates and validated metadata.");assertEquals(1,restored.get("rows").get("foods").asInt());assertEquals(1,restored.get("rows").get("exercises").asInt());assertEquals("2020-01-02",restored.get("dates").get("foodFrom").asText());assertEquals(418.917,restored.get("knownEnergyKj").get("food").asDouble());
                assertNotEquals(0,script(container,db,source,scripts+"verify.sh","--env","FITNUTRI_DATABASE_IDENTITY=wrong_database").status());
                applicationProbe(db,target,true);
                assertEquals(restored,report(script(container,db,target,scripts+"verify.sh")),"Application login/reads must not alter restored rows.");
                assertEquals(original,report(script(container,db,source,scripts+"verify.sh")),"Clone application must never alter source rows.");
                var mode=docker("exec",container,"stat","-c","%a",backup);success(mode);assertEquals("600",mode.output().trim());
                System.out.println("Protected synthetic backup: "+backup);
                System.out.println("Restore reconciliation passed: "+restored.get("rows")+"; known energy kJ "+restored.get("knownEnergyKj"));
            } finally {
                // Name was freshly generated, verified absent, and only this test's restore could create it.
                try(var c=db.connect();var s=c.createStatement()){s.execute("DROP DATABASE IF EXISTS \""+target+"\" WITH (FORCE)");}
            }
        }
    }
}
