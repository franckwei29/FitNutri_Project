package com.fitnutri.backend.records;

import com.fasterxml.jackson.databind.ObjectMapper;
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
class FoodCreationControllerTest {
    private final FoodRecordService service=mock(FoodRecordService.class);private final CurrentUserService current=mock(CurrentUserService.class);private final ObjectMapper json=new ObjectMapper();
    private final UsernamePasswordAuthenticationToken auth=new UsernamePasswordAuthenticationToken("synthetic@example.test","",List.of());
    private final org.springframework.test.web.servlet.MockMvc mvc=MockMvcBuilders.standaloneSetup(new FoodCreationController(service,current)).setControllerAdvice(new ApiExceptionHandler()).build();
    private Map<String,Object> item(){return new LinkedHashMap<>(Map.of("clientEntryId","3d849f70-7c4e-4fdf-abd0-a53c1c807dd9","referenceId","9007199254740993","quantityGrams",150,"mealType","LUNCH"));}
    private Map<String,Object> body(Map<String,Object> item){return new LinkedHashMap<>(Map.of("submissionId","0d9c91d6-8ecb-4a43-bdca-064a0a4e90dd","recordDate","2020-01-01","zone","Australia/Sydney","source","MANUAL","items",List.of(item)));}
    @Test void initial201AndReplay200ReturnMinimalProofAndOnlyServerOwner() throws Exception {
        var user=new User();user.setId(41L);when(current.requireGeneralUser(auth)).thenReturn(user);var id=UUID.fromString("0d9c91d6-8ecb-4a43-bdca-064a0a4e90dd");var proof=List.of(new SubmissionReceiptResponse.RecordIdentity("501",UUID.fromString("3d849f70-7c4e-4fdf-abd0-a53c1c807dd9"),LocalDate.of(2020,1,1),0));when(service.confirmBatch(eq(41L),any())).thenReturn(new SubmissionReceiptResponse(id,SubmissionOperation.FOOD_BATCH,Instant.parse("2026-10-16T00:00:00Z"),false,proof),new SubmissionReceiptResponse(id,SubmissionOperation.FOOD_BATCH,Instant.parse("2026-10-16T00:00:00Z"),true,proof));
        for(int status:new int[]{201,200})mvc.perform(post("/api/records/food/batch").principal(auth).contentType("application/json").content(json.writeValueAsBytes(body(item())))).andExpect(status().is(status)).andExpect(header().string("Idempotent-Replay",Boolean.toString(status==200))).andExpect(header().string("Cache-Control","no-store")).andExpect(jsonPath("$.records[0].id").value("501")).andExpect(jsonPath("$.requestSha256").doesNotExist());verify(service,times(2)).confirmBatch(eq(41L),argThat(r->r.items().getFirst().referenceId().equals("9007199254740993")&&r.items().getFirst().quantityGrams().compareTo(new BigDecimal("150"))==0));
    }
    @Test void clientNutrientsOwnerNameConfidenceAndUnknownItemFieldsNeverReachService() throws Exception {
        for(String field:List.of("calories","energyKj","proteinGrams","userId","foodName","confidence")){var item=item();item.put(field,999);mvc.perform(post("/api/records/food/batch").contentType("application/json").content(json.writeValueAsBytes(body(item)))).andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));var body=body(item());body.put(field,999);mvc.perform(post("/api/records/food/batch").contentType("application/json").content(json.writeValueAsBytes(body))).andExpect(status().isBadRequest());}verifyNoInteractions(service,current);
    }
    @Test void malformedMissingNumericOpaqueIdAndEmptyBatchAreRejected() throws Exception {
        for(Object id:List.of(101,"bad","0","-1")){var item=item();item.put("referenceId",id);mvc.perform(post("/api/records/food/batch").contentType("application/json").content(json.writeValueAsBytes(body(item)))).andExpect(status().isBadRequest());}
        for(Object quantity:List.of("150",true)){var wrong=item();wrong.put("quantityGrams",quantity);mvc.perform(post("/api/records/food/batch").contentType("application/json").content(json.writeValueAsBytes(body(wrong)))).andExpect(status().isBadRequest());}
        var missing=item();missing.remove("mealType");mvc.perform(post("/api/records/food/batch").contentType("application/json").content(json.writeValueAsBytes(body(missing)))).andExpect(status().isBadRequest());var empty=body(item());empty.put("items",List.of());mvc.perform(post("/api/records/food/batch").contentType("application/json").content(json.writeValueAsBytes(empty))).andExpect(status().isBadRequest());verifyNoInteractions(service,current);
    }
    @Test void legacySingleProtocolCannotSubmitClientCalorieAuthority() throws Exception {mvc.perform(post("/api/records/food").contentType("application/json").content("{\"foodName\":\"Fake\",\"quantityGrams\":100,\"mealType\":\"LUNCH\",\"source\":\"MANUAL\",\"calories\":1}")).andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));verifyNoInteractions(service,current);}
}
