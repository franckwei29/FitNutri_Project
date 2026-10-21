package com.fitnutri.backend.submission;
import com.fitnutri.backend.common.*;
import com.fitnutri.backend.user.User;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
class SubmissionV2ControllerTest {
 @Test void currentOwnerOnlyTypedMinimalReceiptIsPrivate()throws Exception{var service=mock(SubmissionService.class);var current=mock(CurrentUserService.class);var user=new User();user.setId(42L);when(current.requireGeneralUser(null)).thenReturn(user);var id=UUID.randomUUID();var client=UUID.randomUUID();var date=LocalDate.of(2020,1,1);when(service.findReceiptV2(42L,id)).thenReturn(new SubmissionReceiptV2Response(2,id,SubmissionOperation.FOOD_BATCH_V2,Instant.parse("2020-01-01T00:00:00Z"),true,List.of(new SubmissionReceiptV2Response.Root("SINGLE","1",client,date,0)),List.of(new SubmissionReceiptV2Response.Leaf("FOOD_COMPONENT","1",client,client,date,0))));var mvc=MockMvcBuilders.standaloneSetup(new SubmissionV2Controller(service,current)).setControllerAdvice(new ApiExceptionHandler()).build();mvc.perform(get("/api/v2/submissions/"+id).param("owner","99")).andExpect(status().isOk()).andExpect(header().string("Cache-Control","no-store")).andExpect(jsonPath("$.schemaVersion").value(2)).andExpect(jsonPath("$.kind").value("FOOD_BATCH_V2")).andExpect(jsonPath("$.roots[0].id").value("1")).andExpect(jsonPath("$.records").doesNotExist()).andExpect(jsonPath("$.nutrients").doesNotExist());verify(service).findReceiptV2(42L,id);}
}
