package com.fitnutri.backend.records;

import com.fitnutri.backend.activity.*;
import com.fitnutri.backend.common.*;
import com.fitnutri.backend.profile.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ExerciseEstimateServiceTest {
    private final ExerciseCatalog catalogue=mock(ExerciseCatalog.class);private final ProfileService profiles=mock(ProfileService.class);private final WeightEntryRepository weights=mock(WeightEntryRepository.class);
    private final ExerciseEstimateService service=new ExerciseEstimateService(catalogue,profiles,weights,new RecordDatePolicy(Clock.fixed(Instant.parse("2026-10-16T00:00:00Z"),ZoneOffset.UTC),new ZoneResolver("Australia/Sydney")));
    private final LocalDate day=LocalDate.of(2020,1,2);
    private final ExerciseType type=type();private final UserProfile profile=profile();
    static ExerciseType type(){var d=new ActivityDataset();d.setId(3L);d.setName("SYNTHETIC");d.setRelease("test");d.setRevision("one");d.setVerified(true);d.setContentSha256("a".repeat(64));d.setMinAge(19);d.setMaxAge(59);var t=new ExerciseType();t.setId(11L);t.setDataset(d);t.setActive(true);t.setActivityKey("synthetic");t.setDisplayName("Synthetic activity");t.setSourceActivityCode("99901");t.setSourceDescription("Synthetic general activity");t.setMetValue(new BigDecimal("3.50"));return t;}
    private UserProfile profile(){var p=new UserProfile();p.setBirthYear(1990);p.setAdultConfirmed(true);p.setOnboardingCompleted(true);return p;}
    private void arrange(BigDecimal kg){when(catalogue.requireImmutable(11L)).thenReturn(type);when(profiles.requireCompletedAdultSetup(1L)).thenReturn(profile);if(kg!=null){var w=new WeightEntry();w.setWeightKg(kg);w.setEntryDate(LocalDate.of(2020,1,1));when(weights.findFirstByUserIdAndEntryDateLessThanEqualOrderByEntryDateDesc(1L,day)).thenReturn(Optional.of(w));}}
    private ExerciseEstimateResponse estimate(String minutes){return service.estimate(1L,11L,day,new BigDecimal(minutes),"Australia/Sydney");}
    @Test void syntheticFormulaHasOneFinalRoundingAndCompleteDatedInputs(){arrange(new BigDecimal("70.00"));var r=estimate("10");assertEquals(new BigDecimal("170.847"),r.estimatedKj());assertEquals(EstimateStatus.AVAILABLE,r.estimateStatus());assertEquals(new BigDecimal("70.00"),r.weightKgUsed());assertEquals(LocalDate.of(2020,1,1),r.weightDateUsed());assertEquals(new BigDecimal("3.50"),r.metUsed());assertNull(r.unavailableReason());verify(weights).findFirstByUserIdAndEntryDateLessThanEqualOrderByEntryDateDesc(1L,day);}
    @Test void fractionalMinutesKeepPrecisionAndDoNotRoundIntermediateKcal(){arrange(new BigDecimal("70.25"));assertEquals(new BigDecimal("180.030"),estimate("10.50").estimatedKj());}
    @Test void missingWeightHasNullEnergyAndNeverFallsBackTo70(){arrange(null);var r=estimate("10");assertEquals(EstimateStatus.MISSING_WEIGHT,r.estimateStatus());assertNull(r.estimatedKj());assertNull(r.weightKgUsed());assertNull(r.weightDateUsed());assertNull(r.metUsed());assertNotNull(r.unavailableReason());}
    @Test void sourceApplicabilityTakesPriorityOverMissingWeight(){arrange(null);profile.setBirthYear(2002);var r=estimate("10");assertEquals(EstimateStatus.NO_APPLICABLE_REFERENCE,r.estimateStatus());assertNull(r.estimatedKj());verifyNoInteractions(weights);}
    @Test void unknownBirthYearAndOutsideBothAgeBoundariesAreUnavailable(){arrange(null);for(Integer birth:Arrays.asList(null,1900,2002)){profile.setBirthYear(birth);assertEquals(EstimateStatus.NO_APPLICABLE_REFERENCE,estimate("10").estimateStatus());}verifyNoInteractions(weights);}
    @Test void exactSourceAgeBoundariesAreInclusiveUsingRecordYearApproximation(){arrange(new BigDecimal("70"));for(int birth:new int[]{2001,1961}){profile.setBirthYear(birth);assertEquals(EstimateStatus.AVAILABLE,estimate("10").estimateStatus());}}
    @Test void invalidMinutesPrecisionBoundsAndFutureDateFailBeforeReferenceRead(){for(String m:new String[]{"0","-1","1440.01","0.001"})assertEquals("VALIDATION_ERROR",assertThrows(BusinessException.class,()->estimate(m)).code());assertThrows(BusinessException.class,()->service.estimate(1L,11L,LocalDate.of(2099,1,1),BigDecimal.TEN,"Australia/Sydney"));verifyNoInteractions(catalogue,profiles,weights);}
    @Test void inactiveOrUnverifiedSourceCannotBecomeANewSelection(){arrange(null);type.setActive(false);assertEquals("REFERENCE_INACTIVE",assertThrows(BusinessException.class,()->estimate("10")).code());type.setActive(true);type.getDataset().setVerified(false);assertEquals("UNSUPPORTED_REFERENCE",assertThrows(BusinessException.class,()->estimate("10")).code());}
    @Test void missingOfficialCodeOrDescriptionCannotBecomeACompleteNewReference(){arrange(null);type.setSourceActivityCode(null);assertEquals("UNSUPPORTED_REFERENCE",assertThrows(BusinessException.class,()->estimate("10")).code());type.setSourceActivityCode("99901");type.setSourceDescription(null);assertEquals("UNSUPPORTED_REFERENCE",assertThrows(BusinessException.class,()->estimate("10")).code());}
    @Test void fingerprintBindsPhysicalInputsButIgnoresDisplaySettingsAndEquivalentDecimalScale(){arrange(new BigDecimal("70.00"));var original=estimate("10.00");assertEquals(original.calculationFingerprint(),estimate("10").calculationFingerprint());profile.setEnergyUnit(EnergyUnit.KCAL);type.setDisplayName("Changed display label");assertEquals(original.calculationFingerprint(),estimate("10").calculationFingerprint());assertNotEquals(original.calculationFingerprint(),estimate("10.01").calculationFingerprint());var w=new WeightEntry();w.setEntryDate(LocalDate.of(2020,1,1));w.setWeightKg(new BigDecimal("71"));when(weights.findFirstByUserIdAndEntryDateLessThanEqualOrderByEntryDateDesc(1L,day)).thenReturn(Optional.of(w));assertNotEquals(original.calculationFingerprint(),estimate("10").calculationFingerprint());}
}
