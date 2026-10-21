package com.fitnutri.backend.common;

import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;
import javax.sql.DataSource;

/** Read-only reviewed-history check, invoked before Hibernate validates new mapped tables. */
public final class SchemaReadinessGate {
    public static final int APPROVED_VERSION=13;
    private static final Set<String> REQUIRED=java.util.stream.IntStream.rangeClosed(1,APPROVED_VERSION).mapToObj(Integer::toString).collect(java.util.stream.Collectors.toUnmodifiableSet());
    public SchemaReadinessGate(DataSource source) {
        var applied=new HashSet<String>();
        try(var connection=source.getConnection();var statement=connection.createStatement();var rows=statement.executeQuery("SELECT version,success FROM public.flyway_schema_history")) {
            while(rows.next())if(!rows.getBoolean(2)||!applied.add(rows.getString(1)))throw blocked();
            if(!applied.equals(REQUIRED))throw blocked();
        } catch(SQLException ignored){throw blocked();}
    }
    private static IllegalStateException blocked(){return new IllegalStateException("Database schema readiness requires the approved V"+APPROVED_VERSION+" migration history. Run the reviewed migration procedure before application startup.");}
}
