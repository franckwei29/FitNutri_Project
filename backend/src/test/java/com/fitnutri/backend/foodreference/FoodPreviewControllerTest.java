package com.fitnutri.backend.foodreference;

import com.fitnutri.backend.common.ApiExceptionHandler;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class FoodPreviewControllerTest {
    private final FoodReferenceRepository repo=mock(FoodReferenceRepository.class);
    private final FoodReferenceService references=new FoodReferenceService(repo);
    private final FoodCalculationService calculator=new FoodCalculationService();
    private final org.springframework.test.web.servlet.MockMvc mvc=MockMvcBuilders.standaloneSetup(new FoodReferenceController(references,calculator,mock(com.fitnutri.backend.records.OwnedPreviewService.class),mock(com.fitnutri.backend.common.CurrentUserService.class))).setControllerAdvice(new ApiExceptionHandler()).build();
    @Test void bothPathsDelegateToIdenticalCanonicalPreviewAndStringIdentity() throws Exception {
        var f=FoodCalculationServiceTest.reference();f.setPublicFoodKey("SYNTHETIC");f.setDisplayName("Synthetic rice");when(repo.findById(f.getId())).thenReturn(Optional.of(f));when(repo.findActiveByPublicFoodKey("SYNTHETIC")).thenReturn(Optional.of(f));
        for(String path:new String[]{"/by-id/9007199254740993/nutrition","/SYNTHETIC/nutrition"}) mvc.perform(get("/api/food-reference"+path).param("grams","150.000")).andExpect(status().isOk()).andExpect(jsonPath("$.reference.referenceId").value("9007199254740993")).andExpect(jsonPath("$.quantityGrams").value(150)).andExpect(jsonPath("$.nutrients.energyKj").value(988.5)).andExpect(jsonPath("$.calculationFingerprint").value(calculator.fingerprint(f,new java.math.BigDecimal("150")))).andExpect(jsonPath("$.calories").doesNotExist());
    }
    @Test void malformedMissingOutOfBoundsAndExcessPrecisionInputsAreStructuredErrors() throws Exception {
        when(repo.findById(1L)).thenReturn(Optional.of(FoodCalculationServiceTest.reference()));
        for(String grams:new String[]{"NaN","Infinity","0","10000.001","0.0001"}) mvc.perform(get("/api/food-reference/by-id/1/nutrition").param("grams",grams)).andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
        mvc.perform(get("/api/food-reference/by-id/1/nutrition")).andExpect(status().isBadRequest());
    }
    @Test void historicalDetailsDoNotGrantPermissionToPreviewInactiveOrIncompleteNewSelection() throws Exception {
        var f=FoodCalculationServiceTest.reference();f.setActive(false);when(repo.findById(1L)).thenReturn(Optional.of(f));mvc.perform(get("/api/food-reference/by-id/1/nutrition").param("grams","100")).andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("REFERENCE_INACTIVE"));f.setActive(true);f.setEnergyKj100g(null);mvc.perform(get("/api/food-reference/by-id/1/nutrition").param("grams","100")).andExpect(status().isUnprocessableEntity());
    }
    @Test void absentReferenceIs404InsteadOfAnInventedFood() throws Exception {mvc.perform(get("/api/food-reference/by-id/1/nutrition").param("grams","100")).andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("NOT_FOUND"));}
}
