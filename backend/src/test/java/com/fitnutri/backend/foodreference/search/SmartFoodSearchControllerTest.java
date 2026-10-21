package com.fitnutri.backend.foodreference.search;
import com.fitnutri.backend.common.*;
import com.fitnutri.backend.user.User;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
class SmartFoodSearchControllerTest {
 private final SmartFoodSearchService search=mock(SmartFoodSearchService.class);private final CurrentUserService current=mock(CurrentUserService.class);
 private final org.springframework.test.web.servlet.MockMvc mvc=MockMvcBuilders.standaloneSetup(new SmartFoodSearchController(search,current,new tools.jackson.databind.ObjectMapper())).setControllerAdvice(new ApiExceptionHandler()).build();
 private void owner(){var user=new User();user.setId(42L);when(current.requireGeneralUser(null)).thenReturn(user);}
 @Test void emptySuccessUsesSessionOwnerAndPrivateCache()throws Exception{owner();when(search.search("rice",Map.of(),"SUGGESTED",0,5,42L)).thenReturn(new SmartFoodSearchService.Response(List.of(),0,0,5,false,null,null));mvc.perform(get("/api/food-reference/smart-search").param("query","rice").param("owner","99")).andExpect(status().isOk()).andExpect(header().string("Cache-Control","no-store")).andExpect(jsonPath("$.totalMatches").value(0)).andExpect(jsonPath("$.results").isEmpty());verify(search).search("rice",Map.of(),"SUGGESTED",0,5,42L);}
 @Test void malformedParametersDoNotReachSearch()throws Exception{owner();for(String facets:List.of("null","[]","{","{\"state\":1}","x".repeat(1001)))mvc.perform(get("/api/food-reference/smart-search").param("query","rice").param("facets",facets)).andExpect(status().isBadRequest());mvc.perform(get("/api/food-reference/smart-search")).andExpect(status().isBadRequest());verifyNoInteractions(search);}
 @Test void inactiveOrAbsentSessionAndServiceFailureStayDistinct()throws Exception{when(current.requireGeneralUser(null)).thenThrow(new BusinessException(HttpStatus.UNAUTHORIZED,"UNAUTHENTICATED","Sign in."));mvc.perform(get("/api/food-reference/smart-search").param("query","rice")).andExpect(status().isUnauthorized());reset(current);owner();when(search.search(anyString(),anyMap(),anyString(),anyInt(),anyInt(),anyLong())).thenThrow(new BusinessException(HttpStatus.SERVICE_UNAVAILABLE,"CATALOGUE_UNAVAILABLE","Try again."));mvc.perform(get("/api/food-reference/smart-search").param("query","rice")).andExpect(status().isServiceUnavailable()).andExpect(jsonPath("$.code").value("CATALOGUE_UNAVAILABLE"));}
}
