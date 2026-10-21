package com.fitnutri.backend.records.food;
import com.fitnutri.backend.common.*;
import com.fitnutri.backend.foodreference.NutrientValues;
import com.fitnutri.backend.submission.*;
import com.fitnutri.backend.user.User;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
class FoodEntryCreationV2ControllerTest {
 final FoodEntryV2Service service=mock(FoodEntryV2Service.class);final CurrentUserService current=mock(CurrentUserService.class);
 final org.springframework.test.web.servlet.MockMvc mvc=MockMvcBuilders.standaloneSetup(new FoodEntryV2Controller(mock(FoodEntryPreviewService.class),current,service,mock(FoodEntryMutationService.class))).setControllerAdvice(new ApiExceptionHandler()).build();
 final UUID submission=UUID.fromString("11111111-1111-4111-8111-111111111111"),client=UUID.fromString("22222222-2222-4222-8222-222222222222");final LocalDate date=LocalDate.of(2020,1,1);
 void owner(){var user=new User();user.setId(42L);when(current.requireGeneralUser(null)).thenReturn(user);}
 String body(){return "{\"submissionId\":\""+submission+"\",\"recordDate\":\"2020-01-01\",\"zone\":\"Australia/Sydney\",\"source\":\"MANUAL\",\"previewFingerprint\":\""+"a".repeat(64)+"\",\"entries\":[{\"kind\":\"SINGLE\",\"clientEntryId\":\""+client+"\",\"referenceId\":\"101\",\"quantityGrams\":100,\"mealType\":\"LUNCH\"}]}";}
 SubmissionReceiptV2Response response(boolean replayed){return new SubmissionReceiptV2Response(2,submission,SubmissionOperation.FOOD_BATCH_V2,Instant.parse("2020-01-01T00:00:00Z"),replayed,List.of(new SubmissionReceiptV2Response.Root("SINGLE","1",client,date,0)),List.of(new SubmissionReceiptV2Response.Leaf("FOOD_COMPONENT","1",client,client,date,0)));}
 @Test void newCommitIs201AndExactReplay200WithPrivateTypedProof()throws Exception{owner();when(service.createBatchV2(eq(42L),any())).thenReturn(response(false),response(true));mvc.perform(post("/api/v2/food/batches").contentType(MediaType.APPLICATION_JSON).content(body())).andExpect(status().isCreated()).andExpect(header().string("Cache-Control","no-store")).andExpect(jsonPath("$.schemaVersion").value(2)).andExpect(jsonPath("$.leaves[0].rootClientEntryId").value(client.toString())).andExpect(header().string("Idempotent-Replay","false")).andExpect(jsonPath("$.replayed").value(false));mvc.perform(post("/api/v2/food/batches").contentType(MediaType.APPLICATION_JSON).content(body())).andExpect(status().isOk()).andExpect(header().string("Idempotent-Replay","true")).andExpect(jsonPath("$.replayed").value(true));}
 @Test void clientNutrientsOwnerAndStringGramsNeverReachCreation()throws Exception{owner();for(String input:List.of(body().replace("\"quantityGrams\":100","\"quantityGrams\":\"100\""),body().replace("\"mealType\":\"LUNCH\"","\"mealType\":\"LUNCH\",\"nutrients\":{}"),body().replace("\"source\":\"MANUAL\"","\"source\":\"MANUAL\",\"ownerId\":99")))mvc.perform(post("/api/v2/food/batches").contentType(MediaType.APPLICATION_JSON).content(input)).andExpect(status().isBadRequest());verifyNoInteractions(service);}
 @Test void changedCalculationReturnsTheCurrentDisplayablePreview()throws Exception{owner();var n=new NutrientValues(new BigDecimal("500"),BigDecimal.ONE,BigDecimal.ZERO,BigDecimal.ONE);var leaf=new FoodSnapshotReader.Leaf(client,"101",new BigDecimal("100"),new BigDecimal("100.000000000000"),n);var preview=new FoodEntryPreviewService.Response(List.of(new FoodSnapshotReader.Preview("SINGLE",client,List.of(leaf),n)),n,"b".repeat(64),FoodEntryPreviewService.POLICY,"c".repeat(64));when(service.createBatchV2(eq(42L),any())).thenThrow(new FoodCalculationChangedV2Exception(preview));mvc.perform(post("/api/v2/food/batches").contentType(MediaType.APPLICATION_JSON).content(body())).andExpect(status().isConflict()).andExpect(header().string("Cache-Control","no-store")).andExpect(jsonPath("$.code").value("CALCULATION_CHANGED")).andExpect(jsonPath("$.currentPreview.fingerprint").value("b".repeat(64))).andExpect(jsonPath("$.currentPreview.totals.energyKj").value(500)).andExpect(jsonPath("$.retryable").value(false));}
}
