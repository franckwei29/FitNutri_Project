package com.fitnutri.backend.records.food;
import com.fitnutri.backend.common.*;
import com.fitnutri.backend.user.User;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
class FoodEntryMutationControllerTest {
 final FoodEntryMutationService mutations=mock(FoodEntryMutationService.class);final CurrentUserService current=mock(CurrentUserService.class);
 final org.springframework.test.web.servlet.MockMvc mvc=MockMvcBuilders.standaloneSetup(new FoodEntryV2Controller(mock(FoodEntryPreviewService.class),current,mock(FoodEntryV2Service.class),mutations)).setControllerAdvice(new ApiExceptionHandler()).build();
 String body(){return "{\"expectedVersion\":0,\"recordDate\":\"2020-01-01\",\"zone\":\"Australia/Sydney\",\"entry\":{\"kind\":\"SINGLE\",\"clientEntryId\":\"22222222-2222-4222-8222-222222222222\",\"referenceId\":\"101\",\"quantityGrams\":100,\"mealType\":\"LUNCH\"}}";}
 void owner(){var user=new User();user.setId(42L);when(current.requireGeneralUser(null)).thenReturn(user);}
 @Test void putAndDeleteUseCurrentOwnerAndRootVersion()throws Exception{owner();var root=new FoodSnapshotReader.Root("7",1,LocalDate.of(2020,1,1),"MANUAL",Instant.parse("2020-01-01T00:00:00Z"),Instant.parse("2020-01-01T01:00:00Z"),com.fitnutri.backend.records.DataQuality.CURRENT,"Synthetic food",Map.of("kind","SINGLE"),null,1,List.of());when(mutations.updateOwnedRoot(eq(42L),eq("single"),eq(7L),any())).thenReturn(root);mvc.perform(put("/api/v2/food/entries/single/7").contentType(MediaType.APPLICATION_JSON).content(body())).andExpect(status().isOk()).andExpect(header().string("Cache-Control","no-store")).andExpect(jsonPath("$.version").value(1));var request=org.mockito.ArgumentCaptor.forClass(FoodEntryInputs.UpdateRequest.class);verify(mutations).updateOwnedRoot(eq(42L),eq("single"),eq(7L),request.capture());org.junit.jupiter.api.Assertions.assertEquals(0L,request.getValue().expectedVersion());mvc.perform(delete("/api/v2/food/entries/single/7").param("expectedVersion","1").param("owner","99")).andExpect(status().isNoContent()).andExpect(header().string("Cache-Control","no-store"));verify(mutations).softDeleteOwnedRoot(42L,"single",7L,1L);}
 @Test void malformedVersionAndExtraAuthorityDoNotReachMutation()throws Exception{owner();for(String value:List.of(body().replace("\"expectedVersion\":0","\"expectedVersion\":0.5"),body().replace("\"expectedVersion\":0","\"expectedVersion\":\"0\""),body().replace("\"expectedVersion\":0","\"expectedVersion\":0,\"source\":\"VOICE\"")))mvc.perform(put("/api/v2/food/entries/single/7").contentType(MediaType.APPLICATION_JSON).content(value)).andExpect(status().isBadRequest());mvc.perform(delete("/api/v2/food/entries/single/7")).andExpect(status().isBadRequest());verifyNoInteractions(mutations);}
}
