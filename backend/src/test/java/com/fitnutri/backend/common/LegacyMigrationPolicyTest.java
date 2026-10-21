package com.fitnutri.backend.common;

import java.util.Map;
import org.flywaydb.core.api.configuration.FluentConfiguration;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
class LegacyMigrationPolicyTest {
    @Test void explicitZoneAndApprovalAreDistinctAndExistingPlaceholdersSurvive() {
        var config=mock(FluentConfiguration.class);when(config.getPlaceholders()).thenReturn(Map.of("other","retained"));
        new LegacyMigrationPolicy("Australia/Sydney",false).customize(config);
        verify(config).placeholders(Map.of("other","retained","legacy_zone","Australia/Sydney","legacy_zone_approved","false"));
    }
    @Test void unconfirmedDefaultCannotMasqueradeAsApproval() {
        var config=mock(FluentConfiguration.class);when(config.getPlaceholders()).thenReturn(Map.of());new LegacyMigrationPolicy("UNCONFIRMED",false).customize(config);verify(config).placeholders(Map.of("legacy_zone","UNCONFIRMED","legacy_zone_approved","false"));
    }
    @Test void invalidOrSqlFragmentZoneIsRejectedBeforeMigrationConfiguration() {
        assertThrows(IllegalArgumentException.class,()->new LegacyMigrationPolicy("Invalid/Zone",true));assertThrows(IllegalArgumentException.class,()->new LegacyMigrationPolicy("Australia/Sydney';DROP TABLE app_user;--",true));
    }
}
