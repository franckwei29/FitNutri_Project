package com.fitnutri.backend.support;

import java.net.URI;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.UUID;
import org.flywaydb.core.Flyway;

/** Owns only a UUID database it created, using explicitly supplied isolated PG16 credentials. */
public final class TestDatabase implements AutoCloseable {
    private final String adminUrl,username,password,name,url;
    private TestDatabase(String adminUrl,String username,String password,String name,String url) {
        this.adminUrl=adminUrl; this.username=username; this.password=password; this.name=name; this.url=url;
    }
    public static TestDatabase create() {
        String adminUrl=required("TEST_DB_ADMIN_URL"),username=required("TEST_DB_USERNAME"),password=required("TEST_DB_PASSWORD");
        URI parsed=URI.create(adminUrl.replaceFirst("^jdbc:",""));
        if(!"postgresql".equals(parsed.getScheme()) || !java.util.Set.of("127.0.0.1","localhost").contains(parsed.getHost())
                || parsed.getPath()==null || !parsed.getPath().startsWith("/fitnutri_integration_") || parsed.getQuery()!=null)
            throw new IllegalStateException("TEST_DB_ADMIN_URL must identify an explicit local fitnutri_integration_* test database.");
        String name="fitnutri_it_"+UUID.randomUUID().toString().replace("-","");
        try(var connection=DriverManager.getConnection(adminUrl,username,password);var statement=connection.createStatement()) {
            if(connection.getMetaData().getDatabaseMajorVersion()!=16 || !"PostgreSQL".equals(connection.getMetaData().getDatabaseProductName()))
                throw new IllegalStateException("Tests require isolated PostgreSQL16.");
            statement.execute("CREATE DATABASE \""+name+"\"");
            String url=adminUrl.substring(0,adminUrl.lastIndexOf('/')+1)+name;
            return new TestDatabase(adminUrl,username,password,name,url);
        } catch(Exception e) { throw new IllegalStateException("Could not create isolated test database.",e); }
    }
    private static String required(String key) {
        String value=System.getenv(key);
        if(value==null || value.isBlank()) throw new IllegalStateException("Set "+key+" explicitly; no shared database fallback is permitted.");
        return value;
    }
    public Connection connect() throws java.sql.SQLException {return DriverManager.getConnection(url,username,password);}
    public Flyway flyway() {return Flyway.configure().dataSource(url,username,password).baselineOnMigrate(false).baselineVersion("1").cleanDisabled(true).placeholders(java.util.Map.of("legacy_zone","UNCONFIRMED","legacy_zone_approved","false")).load();}
    public Flyway flywayAtVersion(String target) {return Flyway.configure().dataSource(url,username,password).baselineOnMigrate(false).baselineVersion("1").target(target).cleanDisabled(true).placeholders(java.util.Map.of("legacy_zone","UNCONFIRMED","legacy_zone_approved","false")).load();}
    public String url() {return url;}
    public String username() {return username;}
    public String password() {return password;}
    @Override public void close() {
        if(!name.matches("fitnutri_it_[0-9a-f]{32}")) throw new IllegalStateException("Refusing to clean unknown database.");
        try(var connection=DriverManager.getConnection(adminUrl,username,password);var statement=connection.createStatement()) {
            statement.execute("DROP DATABASE \""+name+"\" WITH (FORCE)");
        } catch(Exception e) {throw new IllegalStateException("Could not clean owned test database.",e);}
    }
}
