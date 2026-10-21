package com.fitnutri.backend.common;

import java.time.ZoneId;
import java.util.HashMap;
import org.flywaydb.core.api.configuration.FluentConfiguration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.flyway.autoconfigure.FlywayConfigurationCustomizer;
import org.springframework.stereotype.Component;

/** Only allowlisted timezone text is interpolated into migration SQL; no untrusted SQL fragments. */
@Component
public class LegacyMigrationPolicy implements FlywayConfigurationCustomizer {
    private final String zone;
    private final boolean approved;
    public LegacyMigrationPolicy(@Value("${app.migration.legacy-zone:UNCONFIRMED}") String zone,@Value("${app.migration.legacy-zone-approved:false}") boolean approved) {
        if(!"UNCONFIRMED".equals(zone)&&!ZoneId.getAvailableZoneIds().contains(zone)) throw new IllegalArgumentException("Configure a valid IANA legacy migration zone or leave it UNCONFIRMED.");
        this.zone=zone;this.approved=approved;
    }
    @Override public void customize(FluentConfiguration configuration) {
        var placeholders=new HashMap<>(configuration.getPlaceholders());
        placeholders.put("legacy_zone",zone);placeholders.put("legacy_zone_approved",Boolean.toString(approved));configuration.placeholders(placeholders);
    }
}
