package com.fitnutri.backend.food;

import com.fitnutri.backend.ai.ControlledAiProvider;
import com.fitnutri.backend.common.BusinessException;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.util.List;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.*;
import org.springframework.mock.web.MockMultipartFile;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class OpenAiVisionServiceTest {
    private ControlledAiProvider provider;private FoodCandidateMatcher matcher;private OpenAiVisionService vision;private ImageValidator.PreparedImage image;
    @BeforeEach void arrange() throws Exception {provider=new ControlledAiProvider();matcher=mock(FoodCandidateMatcher.class);vision=new OpenAiVisionService(provider.transport,matcher);var bytes=new ByteArrayOutputStream();assertTrue(ImageIO.write(new BufferedImage(2,2,BufferedImage.TYPE_INT_RGB),"png",bytes));image=new ImageValidator().read(new MockMultipartFile("image","synthetic.png","image/png",bytes.toByteArray()));}
    @AfterEach void cleanup(){provider.close();}
    @Test void candidatesComeFromActualProviderJsonAndServerMatcherWithoutConfidenceOrRecordIdentity(){provider.body=ControlledAiProvider.envelope("{\"items\":[{\"name\":\"Rice, white, cooked\",\"suggestedGrams\":150},{\"name\":\"Unknown synthetic dish\",\"suggestedGrams\":null}]}");when(matcher.match(anyString())).thenReturn(List.of());var rows=vision.recognizeFood(image);assertEquals(2,rows.size());assertEquals("Rice, white, cooked",rows.get(0).proposedName());assertEquals(new BigDecimal("150"),rows.get(0).suggestedGrams());assertNull(rows.get(1).suggestedGrams());assertFalse(rows.get(0).candidateKey().equals(rows.get(1).candidateKey()));assertTrue(rows.get(0).referenceMatches().isEmpty());assertFalse(rows.get(0).warnings().isEmpty());assertEquals(1,provider.calls.get());assertTrue(provider.lastRequest.contains("data:image/jpeg;base64,"));assertTrue(provider.lastRequest.contains("json_schema"));assertFalse(provider.lastRequest.contains("synthetic.png"));verify(matcher).match("Rice, white, cooked");verify(matcher).match("Unknown synthetic dish");}
    @Test void noFoodIsAnHonestEmptyResultWithNoInventedFallback(){provider.body=ControlledAiProvider.envelope("{\"items\":[]}");assertTrue(vision.recognizeFood(image).isEmpty());verifyNoInteractions(matcher);}
    @Test void badJsonMissingFieldsNegativeExcessPrecisionIdsAndTooManyItemsAreRejectedBeforeMatching(){for(String value:List.of("not json","{}","{\"items\":[],\"items\":[]}","{\"items\":[{\"name\":\"rice\",\"suggestedGrams\":-1}]}","{\"items\":[{\"name\":\"rice\",\"suggestedGrams\":0.0001}]}","{\"items\":[{\"name\":\"rice\",\"suggestedGrams\":1,\"referenceId\":\"1\"}]}","{\"items\":["+String.join(",",java.util.Collections.nCopies(51,"{\"name\":\"rice\",\"suggestedGrams\":1}"))+"]}")){provider.body=ControlledAiProvider.envelope(value);var e=assertThrows(BusinessException.class,()->vision.recognizeFood(image));assertEquals("PROVIDER_BAD_RESPONSE",e.code());}verifyNoInteractions(matcher);}
    @Test void incompleteToolOutputAndRefusalNeverBecomeCandidates(){for(String value:List.of("{\"status\":\"incomplete\",\"output\":[]}","{\"status\":\"completed\",\"output\":[{\"type\":\"function_call\",\"name\":\"write_database\"}]}")){provider.body=value;assertThrows(BusinessException.class,()->vision.recognizeFood(image));}provider.body="{\"status\":\"completed\",\"output\":[{\"type\":\"message\",\"role\":\"assistant\",\"status\":\"completed\",\"content\":[{\"type\":\"refusal\",\"refusal\":\"private provider text\"}]}]}";var e=assertThrows(BusinessException.class,()->vision.recognizeFood(image));assertEquals("PROVIDER_REFUSED",e.code());assertFalse(e.getMessage().contains("private"));verifyNoInteractions(matcher);}
}
