package com.fitnutri.backend.records;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fitnutri.backend.activity.ExerciseTypeResponse;
import com.fitnutri.backend.common.*;
import com.fitnutri.backend.submission.*;
import com.fitnutri.backend.user.User;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
class ExerciseCreationControllerTest {
    private final ExerciseRecordService service=mock(ExerciseRecordService.class);private final ExerciseEstimateService estimates=mock(ExerciseEstimateService.class);private final CurrentUserService current=mock(CurrentUserService.class);private final ObjectMapper json=new ObjectMapper();
    private final UsernamePasswordAuthenticationToken auth=new UsernamePasswordAuthenticationToken("synthetic@example.test","",List.of());
    private final org.springframework.test.web.servlet.MockMvc mvc=MockMvcBuilders.standaloneSetup(new ExerciseCreationController(service,estimates,current,mock(OwnedPreviewService.class))).setControllerAdvice(new ApiExceptionHandler()).build();
    private Map<String,Object> body(){return new LinkedHashMap<>(Map.of("submissionId","0d9c91d6-8ecb-4a43-bdca-064a0a4e90dd","recordDate","2020-01-02","zone","Australia/Sydney","source","MANUAL","clientEntryId","3d849f70-7c4e-4fdf-abd0-a53c1c807dd9","exerciseTypeId","11","durationMinutes",10,"calculationFingerprint","reviewed-original"));}
    private void owner(){var u=new User();u.setId(41L);when(current.requireGeneralUser(auth)).thenReturn(u);}
    private ExerciseEstimateResponse preview(){return new ExerciseEstimateResponse(ExerciseTypeResponse.from(ExerciseEstimateServiceTest.type()),LocalDate.of(2020,1,2),BigDecimal.TEN,new BigDecimal("195.253"),EstimateStatus.AVAILABLE,null,new BigDecimal("80"),LocalDate.of(2020,1,1),new BigDecimal("3.5"),ExerciseEstimateService.FORMULA_VERSION,"reviewed-current");}
    @Test void changedBasisIsDefinitive409WithValidatedCurrentPreviewAndNoStore() throws Exception {owner();when(service.confirmExercise(eq(41L),any())).thenThrow(new CalculationChangedException(preview()));mvc.perform(post("/api/records/exercise").principal(auth).contentType("application/json").content(json.writeValueAsBytes(body()))).andExpect(status().isConflict()).andExpect(header().string("Cache-Control","no-store")).andExpect(jsonPath("$.code").value("CALCULATION_CHANGED")).andExpect(jsonPath("$.currentPreview.estimatedKj").value(195.253)).andExpect(jsonPath("$.currentPreview.weightKgUsed").value(80)).andExpect(jsonPath("$.currentPreview.calculationFingerprint").value("reviewed-current")).andExpect(jsonPath("$.userId").doesNotExist());verify(service,times(1)).confirmExercise(eq(41L),any());}
    @Test void canonical201AndExactReplay200ReturnOnlyCreationIdentity() throws Exception {owner();var b=body();var rows=List.of(new SubmissionReceiptResponse.RecordIdentity("501",UUID.fromString((String)b.get("clientEntryId")),LocalDate.of(2020,1,2),0));when(service.confirmExercise(eq(41L),any())).thenReturn(new SubmissionReceiptResponse(UUID.fromString((String)b.get("submissionId")),SubmissionOperation.EXERCISE_CREATE,Instant.parse("2026-10-16T00:00:00Z"),false,rows),new SubmissionReceiptResponse(UUID.fromString((String)b.get("submissionId")),SubmissionOperation.EXERCISE_CREATE,Instant.parse("2026-10-16T00:00:00Z"),true,rows));for(int expected:new int[]{201,200})mvc.perform(post("/api/records/exercise").principal(auth).contentType("application/json").content(json.writeValueAsBytes(b))).andExpect(status().is(expected)).andExpect(header().string("Idempotent-Replay",Boolean.toString(expected==200))).andExpect(jsonPath("$.records[0].id").value("501")).andExpect(jsonPath("$.estimatedKj").doesNotExist());}
    @Test void clientEstimateWeightOwnerIntensityAndOldNameCannotBypassCanonicalInputs() throws Exception {
        for(String field:List.of("estimatedKj","estimatedCalories","weightKgUsed","metUsed","userId","exerciseType","intensity")){var b=body();b.put(field,999);mvc.perform(post("/api/records/exercise").contentType("application/json").content(json.writeValueAsBytes(b))).andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));}
        for(String field:List.of("calculationFingerprint","exerciseTypeId","recordDate")){var b=body();b.remove(field);mvc.perform(post("/api/records/exercise").contentType("application/json").content(json.writeValueAsBytes(b))).andExpect(status().isBadRequest());}
        var numeric=body();numeric.put("exerciseTypeId",11);mvc.perform(post("/api/records/exercise").contentType("application/json").content(json.writeValueAsBytes(numeric))).andExpect(status().isBadRequest());var stringDuration=body();stringDuration.put("durationMinutes","10");mvc.perform(post("/api/records/exercise").contentType("application/json").content(json.writeValueAsBytes(stringDuration))).andExpect(status().isBadRequest());verifyNoInteractions(service,estimates,current);
    }
    @Test void previewBindsExplicitDateDurationAndServerOwnerAndNeverResolvesOldNames() throws Exception {owner();when(estimates.estimate(41L,11L,LocalDate.of(2020,1,2),BigDecimal.TEN,"Australia/Sydney")).thenReturn(preview());mvc.perform(get("/api/records/exercise/estimate").principal(auth).param("exerciseTypeId","11").param("durationMinutes","10").param("recordDate","2020-01-02").param("zone","Australia/Sydney")).andExpect(status().isOk()).andExpect(jsonPath("$.exerciseType.id").value("11")).andExpect(jsonPath("$.recordDate").value("2020-01-02")).andExpect(jsonPath("$.weightDateUsed").value("2020-01-01"));mvc.perform(get("/api/records/exercise/estimate").param("exerciseType","Walking").param("durationMinutes","10")).andExpect(status().isBadRequest());verifyNoInteractions(service);}
}
