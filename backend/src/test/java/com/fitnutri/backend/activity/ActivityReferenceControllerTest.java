package com.fitnutri.backend.activity;

import com.fitnutri.backend.common.*;
import com.fitnutri.backend.records.ExerciseCatalog;
import java.math.BigDecimal;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
class ActivityReferenceControllerTest {
    private final ExerciseTypeRepository types=mock(ExerciseTypeRepository.class);private final ExerciseCatalog catalogue=new ExerciseCatalog(types);
    private final org.springframework.test.web.servlet.MockMvc mvc=MockMvcBuilders.standaloneSetup(new ActivityReferenceController(catalogue)).setControllerAdvice(new ApiExceptionHandler()).build();
    @Test void unavailableCatalogueIs503RatherThanLegacyNamesAndMetDefaults() throws Exception {mvc.perform(get("/api/records/exercise/types")).andExpect(status().isServiceUnavailable()).andExpect(jsonPath("$.code").value("CATALOGUE_UNAVAILABLE"));}
    @Test void structuredSourceIdentityAgeRangeAndMetAreReturnedWithoutNameGuessing() throws Exception {
        var d=new ActivityDataset();d.setId(7L);d.setName("SYNTHETIC");d.setRelease("test");d.setRevision("one");d.setVerified(true);d.setMinAge(19);d.setMaxAge(59);var t=new ExerciseType();t.setId(9007199254740993L);t.setActivityKey("synthetic");t.setDisplayName("Synthetic activity");t.setSourceActivityCode("99901");t.setSourceDescription("Synthetic example only");t.setMetValue(new BigDecimal("3.50"));t.setActive(true);t.setDataset(d);when(types.findByActiveTrueAndDatasetVerifiedTrue()).thenReturn(List.of(t));
        mvc.perform(get("/api/records/exercise/types")).andExpect(status().isOk()).andExpect(jsonPath("$[0].id").value("9007199254740993")).andExpect(jsonPath("$[0].metValue").value(3.5)).andExpect(jsonPath("$[0].sourceActivityCode").value("99901")).andExpect(jsonPath("$[0].dataset.minAge").value(19)).andExpect(jsonPath("$[0].dataset.maxAge").value(59)).andExpect(jsonPath("$[0].dataset.nutrientPolicy").value("MET_REFERENCE"));
    }
    @Test void absentNumericIdentityIs404AndInvalidIdentityNeverQueriesRepository() {assertEquals("VALIDATION_ERROR",assertThrows(BusinessException.class,()->catalogue.requireImmutable(0L)).code());verifyNoInteractions(types);assertEquals("NOT_FOUND",assertThrows(BusinessException.class,()->catalogue.requireImmutable(99L)).code());}
}
