package com.fitnutri.backend.foodreference;

import com.fitnutri.backend.common.BusinessException;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class FoodCalculationServiceTest {
    private final FoodCalculationService calculator=new FoodCalculationService();
    static FoodReference reference() {
        var d=new FoodDataset();d.setId(2L);d.setVerified(true);d.setContentSha256("a".repeat(64));
        var f=new FoodReference();f.setId(9007199254740993L);f.setDataset(d);f.setActive(true);f.setEnergyKj100g(new BigDecimal("659.000"));f.setProteinG100g(new BigDecimal("3.100"));f.setFatG100g(new BigDecimal("0.200"));f.setCarbsG100g(new BigDecimal("34.700"));return f;
    }
    @Test void hundredAndOneHundredFiftyGramsUseTheSameCanonicalCoreValues() {
        var f=reference();var hundred=calculator.calculate(f,new BigDecimal("100"));var portion=calculator.calculate(f,new BigDecimal("150"));
        assertEquals(new BigDecimal("659.000"),hundred.energyKj());assertEquals(new BigDecimal("988.500"),portion.energyKj());assertEquals(new BigDecimal("4.650"),portion.proteinGrams());assertEquals(new BigDecimal("0.300"),portion.fatGrams());assertEquals(new BigDecimal("52.050"),portion.carbsGrams());
    }
    @Test void finalHalfUpRoundingKeepsExactDecimalBoundaryAndTrueZero() {
        var f=reference();f.setEnergyKj100g(new BigDecimal("0.005"));f.setProteinG100g(BigDecimal.ZERO);f.setFatG100g(new BigDecimal("0.004"));
        var result=calculator.calculate(f,new BigDecimal("10"));assertEquals(new BigDecimal("0.001"),result.energyKj());assertEquals(new BigDecimal("0.000"),result.proteinGrams());assertEquals(new BigDecimal("0.000"),result.fatGrams());
    }
    @Test void onlyFinalResultIsRoundedForFractionalGrams() {
        var f=reference();f.setEnergyKj100g(new BigDecimal("123.456"));assertEquals(new BigDecimal("0.412"),calculator.calculate(f,new BigDecimal("0.334")).energyKj());
    }
    @Test void boundsAndSignificantPrecisionAreCheckedWithoutRoundingInput() {
        var f=reference();for(var grams:new String[]{"0","-1","10000.001","0.0001","10.0001"}) assertEquals("VALIDATION_ERROR",assertThrows(BusinessException.class,()->calculator.calculate(f,new BigDecimal(grams))).code());
        assertThrows(BusinessException.class,()->calculator.calculate(f,null));assertNotNull(calculator.calculate(f,new BigDecimal("0.001")));assertNotNull(calculator.calculate(f,new BigDecimal("10000.0000")));
    }
    @Test void incompleteNegativeUnverifiedAndInactiveReferencesAreNeverSilentlyZeroed() {
        var f=reference();f.setProteinG100g(null);assertUnsupported(f);f.setProteinG100g(new BigDecimal("-0.001"));assertUnsupported(f);f.setProteinG100g(new BigDecimal("1.0001"));assertUnsupported(f);f=reference();f.getDataset().setVerified(false);assertUnsupported(f);f=reference();f.setActive(false);var inactive=f;assertEquals("REFERENCE_INACTIVE",assertThrows(BusinessException.class,()->calculator.calculate(inactive,new BigDecimal("100"))).code());assertUnsupported(null);
    }
    private void assertUnsupported(FoodReference f) {assertEquals("UNSUPPORTED_REFERENCE",assertThrows(BusinessException.class,()->calculator.calculate(f,new BigDecimal("100"))).code());}
    @Test void ownerValidatedHistoricalCalculationCanRetainInactiveVerifiedOriginalReference() {
        var f=reference();f.setActive(false);assertEquals(new BigDecimal("659.000"),calculator.calculateHistorical(f,new BigDecimal("100")).energyKj());f.getDataset().setVerified(false);assertThrows(BusinessException.class,()->calculator.calculateHistorical(f,new BigDecimal("100")));
    }
    @Test void fingerprintIsStableForNumericEquivalenceAndChangesOnlyWithCalculationInputs() {
        var f=reference();String original=calculator.fingerprint(f,new BigDecimal("150.000"));assertEquals(original,calculator.fingerprint(f,new BigDecimal("150")));f.setDisplayName("New display label");assertEquals(original,calculator.fingerprint(f,new BigDecimal("150")));assertNotEquals(original,calculator.fingerprint(f,new BigDecimal("150.001")));f.setEnergyKj100g(new BigDecimal("660"));assertNotEquals(original,calculator.fingerprint(f,new BigDecimal("150")));assertTrue(original.matches("[a-f0-9]{64}"));
    }
    @Test void aPortionExceedingRecordPrecisionIsRejectedBeforeDatabasePersistence() {
        var f=reference();f.setEnergyKj100g(new BigDecimal("999999999.999"));assertThrows(BusinessException.class,()->calculator.calculate(f,new BigDecimal("10000")));
    }
}
