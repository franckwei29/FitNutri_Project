package com.fitnutri.backend.common;

import org.junit.jupiter.api.Test;
import java.time.*;
import static org.junit.jupiter.api.Assertions.*;

class RecordDatePolicyTest {
    private final Clock clock = Clock.fixed(Instant.parse("2026-10-03T14:30:00Z"), ZoneOffset.UTC);
    private final RecordDatePolicy policy = new RecordDatePolicy(clock, new ZoneResolver("Australia/Sydney"));

    @Test void todayUsesRequestedZoneAcrossSydneyDstMidnight() {
        assertEquals(LocalDate.of(2026,10,4), policy.today("Australia/Sydney"));
        assertEquals(LocalDate.of(2026,10,3), policy.today("UTC"));
    }
    @Test void missingZoneUsesConfiguredDefault() {
        assertEquals(LocalDate.of(2026,10,4), policy.today(null));
        assertEquals(LocalDate.of(2026,10,4), policy.today(" "));
    }
    @Test void savedBusinessDateDoesNotMoveWithQueryZone() {
        LocalDate saved = LocalDate.of(2026,10,2);
        assertEquals(saved, policy.requireDate(saved,"UTC"));
        assertEquals(saved, policy.requireDate(saved,"Australia/Sydney"));
    }
    @Test void futureAndMissingDatesAreRejected() {
        assertEquals("VALIDATION_ERROR", assertThrows(BusinessException.class, () -> policy.requireDate(LocalDate.of(2026,10,4),"UTC")).code());
        assertEquals("VALIDATION_ERROR", assertThrows(BusinessException.class, () -> policy.requireDate(null,"UTC")).code());
    }
    @Test void invalidZoneDoesNotEchoUntrustedInput() {
        BusinessException error=assertThrows(BusinessException.class, () -> policy.today("secret/not-a-zone"));
        assertEquals(400,error.status().value());
        assertFalse(error.getMessage().contains("secret"));
    }
    @Test void rangesAreInclusiveAndBounded() {
        LocalDate end=LocalDate.of(2026,10,3);
        assertEquals(366,policy.requireRange(end.minusDays(365),end,"UTC").dayCount());
        assertEquals(1,policy.requireRange(end,end,"UTC").dayCount());
        assertThrows(BusinessException.class, () -> policy.requireRange(end.minusDays(366),end,"UTC"));
        assertThrows(BusinessException.class, () -> policy.requireRange(end,end.minusDays(1),"UTC"));
        assertThrows(BusinessException.class, () -> policy.requireRange(null,end,"UTC"));
    }
}
