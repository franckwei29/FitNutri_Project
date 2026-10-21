package com.fitnutri.backend.records;

import com.fitnutri.backend.common.*;
import java.net.URI;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.http.HttpMethod;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class LegacyWriteGateTest {
    @ParameterizedTest @CsvSource({"PUT,/api/records/food/1","DELETE,/api/records/food/1","PUT,/api/records/exercise/1","DELETE,/api/records/exercise/1"})
    void oldNutrientWritesHardDeletesAndDefaultWeightEstimatesCannotExecute(String method,String path) throws Exception {
        var foods=mock(FoodRecordService.class);var exercises=mock(ExerciseRecordService.class);var current=mock(CurrentUserService.class);
        var mvc=MockMvcBuilders.standaloneSetup(new RecordCorrectionController(foods,exercises,current)).setControllerAdvice(new ApiExceptionHandler()).build();
        mvc.perform(request(HttpMethod.valueOf(method),URI.create(path)).contentType("application/json").content(method.equals("PUT")?"{\"calories\":999}":"")).andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
        verifyNoInteractions(foods,exercises,current);
    }
}
