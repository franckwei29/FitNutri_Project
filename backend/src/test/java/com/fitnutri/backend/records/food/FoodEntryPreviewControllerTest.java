package com.fitnutri.backend.records.food;
import com.fitnutri.backend.common.*;
import com.fitnutri.backend.user.User;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
class FoodEntryPreviewControllerTest {
 private final FoodEntryPreviewService service=mock(FoodEntryPreviewService.class);private final CurrentUserService current=mock(CurrentUserService.class);
 private final org.springframework.test.web.servlet.MockMvc mvc=MockMvcBuilders.standaloneSetup(new FoodEntryV2Controller(service,current,mock(FoodEntryV2Service.class),mock(FoodEntryMutationService.class))).setControllerAdvice(new ApiExceptionHandler()).build();
 private String payload(){return "{\"recordDate\":\"2026-10-17\",\"zone\":\"Australia/Sydney\",\"source\":\"MANUAL\",\"entries\":[{\"kind\":\"SINGLE\",\"clientEntryId\":\"11111111-1111-4111-8111-111111111111\",\"referenceId\":\"1\",\"quantityGrams\":100,\"mealType\":\"LUNCH\"}]}";}
 @Test void typedJsonNumbersAndIdentitiesReachOnlyTheCurrentOwner()throws Exception{var user=new User();user.setId(42L);when(current.requireGeneralUser(null)).thenReturn(user);mvc.perform(post("/api/v2/food/preview").contentType(MediaType.APPLICATION_JSON).content(payload())).andExpect(status().isOk()).andExpect(header().string("Cache-Control","no-store"));var input=org.mockito.ArgumentCaptor.forClass(FoodEntryInputs.PreviewRequest.class);verify(service).previewNewBatch(eq(42L),input.capture());var single=(FoodEntryInputs.Single)input.getValue().entries().getFirst();org.junit.jupiter.api.Assertions.assertEquals("1",single.referenceId());org.junit.jupiter.api.Assertions.assertEquals(new java.math.BigDecimal("100"),single.quantityGrams());}
 @Test void authoritativeExtraFieldsAndStringNumbersAreRejectedByDecode()throws Exception{for(String body:List.of(payload().replace("\"source\":\"MANUAL\"","\"source\":0"),payload().replace("\"mealType\":\"LUNCH\"","\"mealType\":1"),payload().replace("\"quantityGrams\":100","\"quantityGrams\":\"100\""),payload().replace("\"referenceId\":\"1\"","\"referenceId\":1"),payload().replace("\"mealType\":\"LUNCH\"","\"mealType\":\"LUNCH\",\"nutrients\":{}"),payload().replace("\"kind\":\"SINGLE\"","\"kind\":\"DISHLESS\""),payload().replace("\"source\":\"MANUAL\"","\"source\":\"MANUAL\",\"ownerId\":99")))mvc.perform(post("/api/v2/food/preview").contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isBadRequest());verifyNoInteractions(service);}
 @Test void typedEntryCanRoundTripWithoutLosingItsDiscriminator(){var mapper=new tools.jackson.databind.ObjectMapper();var request=mapper.readValue(payload(),FoodEntryInputs.PreviewRequest.class);var encoded=mapper.writeValueAsString(request);org.junit.jupiter.api.Assertions.assertTrue(encoded.contains("\"kind\":\"SINGLE\""));org.junit.jupiter.api.Assertions.assertEquals(request,mapper.readValue(encoded,FoodEntryInputs.PreviewRequest.class));}
}
