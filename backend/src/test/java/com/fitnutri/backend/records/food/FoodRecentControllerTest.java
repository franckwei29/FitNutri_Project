package com.fitnutri.backend.records.food;
import com.fitnutri.backend.common.*;
import com.fitnutri.backend.user.User;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
class FoodRecentControllerTest {
 @Test void currentOwnerAndDefaultSectionLimitAreUsedAndNeverCached()throws Exception{var recent=mock(FoodRecentService.class);var current=mock(CurrentUserService.class);var user=new User();user.setId(42L);when(current.requireGeneralUser(null)).thenReturn(user);when(recent.recentSinglesAndDishes(42L,5)).thenReturn(new FoodSnapshotReader.Page(List.of(),0,0,10,false));var mvc=MockMvcBuilders.standaloneSetup(new FoodRecentController(recent,current)).setControllerAdvice(new ApiExceptionHandler()).build();mvc.perform(get("/api/v2/food/recent").param("userId","99")).andExpect(status().isOk()).andExpect(header().string("Cache-Control","no-store")).andExpect(jsonPath("$.results").isEmpty());verify(recent).recentSinglesAndDishes(42L,5);when(current.requireGeneralUser(null)).thenThrow(new BusinessException(HttpStatus.FORBIDDEN,"FORBIDDEN","User account required."));mvc.perform(get("/api/v2/food/recent")).andExpect(status().isForbidden());}
}
