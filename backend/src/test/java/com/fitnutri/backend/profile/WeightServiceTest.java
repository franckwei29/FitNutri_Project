package com.fitnutri.backend.profile;

import com.fitnutri.backend.common.*;
import com.fitnutri.backend.user.*;
import jakarta.validation.Validation;
import java.math.BigDecimal;
import java.time.*;
import java.util.Optional;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class WeightServiceTest {
    private static final jakarta.validation.ValidatorFactory VALIDATION=Validation.buildDefaultValidatorFactory();
    private final UserRepository users=mock(UserRepository.class);
    private final UserProfileRepository profiles=mock(UserProfileRepository.class);
    private final WeightEntryRepository weights=mock(WeightEntryRepository.class);
    private WeightService service;
    @BeforeEach void arrange() {
        var clock=Clock.fixed(Instant.parse("2026-10-16T08:00:00Z"),ZoneOffset.UTC);
        service=new WeightService(users,profiles,weights,new RecordDatePolicy(clock,new ZoneResolver("Australia/Sydney")),VALIDATION.getValidator(),clock);
    }
    @AfterAll static void closeValidator() {VALIDATION.close();}
    @Test void invalidPrecisionRangeAndFutureDateFailBeforeLocking() {
        assertThrows(BusinessException.class,()->service.upsertByDate(1L,null));
        for(String value:java.util.List.of("19.99","400.01","70.001")) assertThrows(BusinessException.class,()->service.upsertByDate(1L,new AddWeightRequest(LocalDate.of(2020,1,1),new BigDecimal(value),null,"Australia/Sydney")));
        assertThrows(BusinessException.class,()->service.upsertByDate(1L,new AddWeightRequest(LocalDate.of(2090,1,1),new BigDecimal("70"),null,"Australia/Sydney")));
        verifyNoInteractions(users,profiles,weights);
    }
    @Test void disabledOrAdminAccountCannotWriteEvenWhenCallingServiceDirectly() {
        var user=new User();user.setEnabled(false);when(users.lockById(1L)).thenReturn(Optional.of(user));
        var request=new AddWeightRequest(LocalDate.of(2020,1,1),new BigDecimal("70"),null,"Australia/Sydney");
        assertEquals("UNAUTHENTICATED",assertThrows(BusinessException.class,()->service.upsertByDate(1L,request)).code());
        user.setEnabled(true);user.setRole(User.Role.ADMIN);assertEquals("FORBIDDEN",assertThrows(BusinessException.class,()->service.upsertByDate(1L,request)).code());verifyNoInteractions(profiles,weights);
    }
    @Test void staleVersionRejectsIdenticalValueInsteadOfSilentlyAcknowledgingCorrection() {
        var user=new User();when(users.lockById(1L)).thenReturn(Optional.of(user));
        var weight=WeightEntry.of(user,new BigDecimal("70"),LocalDate.of(2020,1,1));weight.setId(2L);weight.setVersion(3);
        when(weights.findByUserIdAndEntryDate(1L,weight.getEntryDate())).thenReturn(Optional.of(weight));
        var error=assertThrows(WeightConflictException.class,()->service.upsertByDate(1L,new AddWeightRequest(weight.getEntryDate(),new BigDecimal("70"),2L,"Australia/Sydney")));
        assertEquals(3,error.currentItem().version());assertEquals("2",error.currentItem().id());verify(weights,never()).saveAndFlush(any());verifyNoInteractions(profiles);
    }
}
