package com.fitnutri.backend.support;

import com.fitnutri.backend.foodreference.FoodCalculationService;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class UxV3FixtureSmokeTest {
    @Test void syntheticRecipeHasIndependentFullRecipeAndPersonalExpectedValues() {
        var references=UxV3Fixtures.syntheticFoodRefs();var calculator=new FoodCalculationService();
        var a=calculator.calculate(references.get(0),new BigDecimal("300"));
        var b=calculator.calculate(references.get(1),new BigDecimal("150"));
        assertEquals(new BigDecimal("3600.000"),a.energyKj().add(b.energyKj()));
        assertEquals(LocalDate.of(2026,10,17),LocalDate.now(UxV3Fixtures.CLOCK));
        assertEquals(new BigDecimal("70"),UxV3Fixtures.WEIGHT_KG);
        assertEquals(3,references.stream().map(r->r.getId()).distinct().count());
        assertTrue(references.stream().allMatch(r->r.getPublicFoodKey().startsWith("SYNTHETIC-")));
    }
}
