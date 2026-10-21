package com.fitnutri.backend.records;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fitnutri.backend.common.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
class RecordCorrectionControllerTest {
    private final FoodRecordService foods=mock(FoodRecordService.class);private final ExerciseRecordService exercises=mock(ExerciseRecordService.class);private final CurrentUserService current=mock(CurrentUserService.class);private final ObjectMapper json=new ObjectMapper();
    private final org.springframework.test.web.servlet.MockMvc mvc=MockMvcBuilders.standaloneSetup(new RecordCorrectionController(foods,exercises,current)).setControllerAdvice(new ApiExceptionHandler()).build();
    private Map<String,Object> food(){return new LinkedHashMap<>(Map.of("expectedVersion",0,"referenceId","101","quantityGrams",100,"mealType","LUNCH","recordDate","2020-01-01","zone","Australia/Sydney"));}
    private Map<String,Object> exercise(){return new LinkedHashMap<>(Map.of("expectedVersion",0,"exerciseTypeId","11","durationMinutes",10,"recordDate","2020-01-01","zone","Australia/Sydney"));}
    @Test void missingOrFractionalVersionAndClientAuthorityCannotReachFoodMutation() throws Exception {
        for(Object version:List.of("0",0.9,-1)){var body=food();body.put("expectedVersion",version);mvc.perform(put("/api/records/food/1").contentType("application/json").content(json.writeValueAsBytes(body))).andExpect(status().isBadRequest());}
        var missing=food();missing.remove("expectedVersion");mvc.perform(put("/api/records/food/1").contentType("application/json").content(json.writeValueAsBytes(missing))).andExpect(status().isBadRequest());
        for(String field:List.of("source","userId","energyKj","calories","confidence")){var body=food();body.put(field,999);mvc.perform(put("/api/records/food/1").contentType("application/json").content(json.writeValueAsBytes(body))).andExpect(status().isBadRequest());}verifyNoInteractions(foods,exercises,current);
    }
    @Test void exerciseUpdateMayOmitFingerprintForServerJudgedNoOpButNeverAcceptsClientSnapshots() throws Exception {
        for(String field:List.of("estimatedKj","weightKgUsed","metUsed","source","userId")){var body=exercise();body.put(field,999);mvc.perform(put("/api/records/exercise/1").contentType("application/json").content(json.writeValueAsBytes(body))).andExpect(status().isBadRequest());}var body=exercise();body.put("expectedVersion",0.5);mvc.perform(put("/api/records/exercise/1").contentType("application/json").content(json.writeValueAsBytes(body))).andExpect(status().isBadRequest());verifyNoInteractions(foods,exercises,current);
    }
    @Test void deleteWithoutRequiredVersionOrWithMalformedVersionIs400BeforeService() throws Exception {for(String kind:List.of("food","exercise")){mvc.perform(delete("/api/records/"+kind+"/1")).andExpect(status().isBadRequest());mvc.perform(delete("/api/records/"+kind+"/1").param("expectedVersion","0.5")).andExpect(status().isBadRequest());}verifyNoInteractions(foods,exercises,current);}
}
