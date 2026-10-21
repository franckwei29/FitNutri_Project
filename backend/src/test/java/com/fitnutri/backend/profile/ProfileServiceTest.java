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

class ProfileServiceTest {
    private static final jakarta.validation.ValidatorFactory VALIDATION=Validation.buildDefaultValidatorFactory();
    private final UserRepository users=mock(UserRepository.class);
    private final UserProfileRepository profiles=mock(UserProfileRepository.class);
    private final WeightEntryRepository weights=mock(WeightEntryRepository.class);
    private final Clock clock=Clock.fixed(Instant.parse("2026-10-16T08:00:00Z"),ZoneOffset.UTC);
    private ProfileService service;
    @BeforeEach void arrange() {service=new ProfileService(users,profiles,weights,new RecordDatePolicy(clock,new ZoneResolver("Australia/Sydney")),VALIDATION.getValidator(),clock);}
    @AfterAll static void closeValidator() {VALIDATION.close();}
    private ProfileSettingsRequest settings(int birthYear,boolean adult,Boolean consent) {
        return new ProfileSettingsRequest(0L,null," Synthetic ",birthYear,adult,Sex.OTHER,new BigDecimal("170"),ActivityLevel.LIGHT,null,null,EnergyUnit.KJ,HealthSafetyAnswer.NO,consent,"Australia/Sydney");
    }
    @Test void adultYearBoundaryAccepts18ButRejects17BeforeAnyWrite() {
        // At the accepted boundary the request reaches the identity lookup; underage input does not.
        assertEquals("UNAUTHENTICATED",assertThrows(BusinessException.class,()->service.updateSettings(1L,settings(2008,true,false))).code());
        clearInvocations(users);
        assertEquals("VALIDATION_ERROR",assertThrows(BusinessException.class,()->service.updateSettings(1L,settings(2009,true,false))).code());
        verifyNoInteractions(users,profiles,weights);
    }
    @Test void missingBodyOrConsentAndUnconfirmedAdultCannotReachPersistence() {
        assertThrows(BusinessException.class,()->service.updateSettings(1L,null));
        assertThrows(BusinessException.class,()->service.updateSettings(1L,settings(1990,false,false)));
        assertThrows(BusinessException.class,()->service.updateSettings(1L,settings(1990,true,null)));
        verifyNoInteractions(users,profiles,weights);
    }
    @Test void completedAdultGuardDoesNotTrustCompletionFlagAlone() {
        assertEquals("ONBOARDING_REQUIRED",assertThrows(BusinessException.class,()->service.requireCompletedAdultSetup(1L)).code());
        var p=new UserProfile();p.setOnboardingCompleted(true);when(profiles.findById(1L)).thenReturn(Optional.of(p));
        assertThrows(BusinessException.class,()->service.requireCompletedAdultSetup(1L));
        p.setAdultConfirmed(true);assertSame(p,service.requireCompletedAdultSetup(1L));verifyNoInteractions(weights);
    }
    @Test void disabledAndAdminAccountsFailInsideLockedTransactionAsWellAsAtController() {
        var user=new User();user.setEnabled(false);when(users.lockById(1L)).thenReturn(Optional.of(user));
        assertEquals("UNAUTHENTICATED",assertThrows(BusinessException.class,()->service.updateSettings(1L,settings(1990,true,false))).code());
        user.setEnabled(true);user.setRole(User.Role.ADMIN);
        assertEquals("FORBIDDEN",assertThrows(BusinessException.class,()->service.updateSettings(1L,settings(1990,true,false))).code());
        verifyNoInteractions(profiles,weights);
    }
    @Test void derivedEligibilityIgnoresStaleLegacyFlagAndRequiresExplicitConsent() {
        var p=new UserProfile();p.setHealthSafetyAnswer(HealthSafetyAnswer.NO);p.setAiQaEligible(true);
        assertFalse(QaEligibility.isEligible(p));p.setHealthSafetyConsent(false);assertFalse(QaEligibility.isEligible(p));
        p.setHealthSafetyConsent(true);p.setAiQaEligible(false);assertTrue(QaEligibility.isEligible(p));
        p.setHealthSafetyAnswer(HealthSafetyAnswer.PREFER_NOT_TO_ANSWER);assertFalse(QaEligibility.isEligible(p));assertFalse(QaEligibility.isEligible(null));
    }
}
