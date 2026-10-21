package com.fitnutri.backend.capabilities;
import com.fitnutri.backend.ai.AiFeature;
import com.fitnutri.backend.common.*;
import com.fitnutri.backend.food.*;
import com.fitnutri.backend.user.User;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import java.util.List;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
class LegacyRecognitionGateTest {
    @Test void unavailableRecognitionCannotCallProviderEvenWithAnUploadedFile() throws Exception {
        var vision=mock(OpenAiVisionService.class);var capabilities=mock(CapabilitiesService.class);var current=mock(CurrentUserService.class);var auth=new UsernamePasswordAuthenticationToken("synthetic@example.test","",List.of());var user=new User();user.setId(41L);when(current.requireGeneralUser(auth)).thenReturn(user);doThrow(new BusinessException(HttpStatus.SERVICE_UNAVAILABLE,"FEATURE_NOT_CONFIGURED","This AI feature is not configured.")).when(capabilities).requireAvailable(AiFeature.IMAGE,user);
        var mvc=MockMvcBuilders.standaloneSetup(new FoodRecognitionController(vision,capabilities,current,mock(ImageValidator.class))).setControllerAdvice(new ApiExceptionHandler()).build();mvc.perform(multipart("/api/food/recognize").file(new MockMultipartFile("image","synthetic.jpg","image/jpeg",new byte[]{1,2,3})).principal(auth)).andExpect(status().isServiceUnavailable()).andExpect(jsonPath("$.code").value("FEATURE_NOT_CONFIGURED"));verifyNoInteractions(vision);
    }
    @Test void missingMultipartImageIs400BeforeAnyFeatureOrProviderCall() throws Exception {
        var vision=mock(OpenAiVisionService.class);var capabilities=mock(CapabilitiesService.class);var current=mock(CurrentUserService.class);var mvc=MockMvcBuilders.standaloneSetup(new FoodRecognitionController(vision,capabilities,current,mock(ImageValidator.class))).setControllerAdvice(new ApiExceptionHandler()).build();mvc.perform(multipart("/api/food/recognize")).andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));verifyNoInteractions(vision,capabilities,current);
    }
}
