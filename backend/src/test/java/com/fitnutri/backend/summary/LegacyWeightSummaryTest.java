package com.fitnutri.backend.summary;

import com.fitnutri.backend.common.*;
import com.fitnutri.backend.profile.*;
import com.fitnutri.backend.user.User;
import java.math.BigDecimal;
import java.time.*;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** Retains the original decimal-weight/display-rounding regression through the canonical facts API. */
class LegacyWeightSummaryTest {
    @Test void decimalWeightMigrationPreservesSummaryValuesAndRoundsDifferenceOnce(){
        var repository=mock(SummaryRepository.class);var profiles=mock(UserProfileRepository.class);var weights=mock(WeightEntryRepository.class);var current=mock(CurrentUserService.class);var user=new User();user.setId(7L);user.setEmail("fixture@example.test");var auth=UsernamePasswordAuthenticationToken.authenticated(user.getEmail(),null,List.of());when(current.requireGeneralUser(auth)).thenReturn(user);
        when(weights.findByUserIdAndEntryDateBetweenOrderByEntryDateDesc(7L,LocalDate.of(2025,12,25),LocalDate.of(2026,1,7))).thenReturn(List.of(WeightEntry.of(user,new BigDecimal("80.17"),LocalDate.of(2026,1,7)),WeightEntry.of(user,new BigDecimal("70.12"),LocalDate.of(2026,1,1))));
        var service=new SummaryService(repository,profiles,weights,new RecordDatePolicy(Clock.fixed(Instant.parse("2026-10-16T00:00:00Z"),ZoneOffset.UTC),new ZoneResolver("Australia/Sydney")),new RuleSummaryService());var controller=new DataSummaryController(service,current);var result=controller.weekly(LocalDate.of(2026,1,7),"Australia/Sydney",auth);var change=result.current().facts().stream().filter(f->f.code().equals("RECORDED_WEIGHT_CHANGE")).findFirst().orElseThrow();
        assertEquals(new BigDecimal("70.12"),change.values().get("fromKg"));assertEquals(new BigDecimal("80.17"),change.values().get("toKg"));assertEquals(new BigDecimal("10.05"),change.values().get("changeKg"));assertTrue(change.text().contains("+10.1 kg"));assertEquals(0,result.current().totals().foodCount());
    }
}
