package com.fitnutri.backend.admin;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fitnutri.backend.common.ApiExceptionHandler;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
class AdminControllerTest {
    final AdminService service=mock(AdminService.class);
    final ObjectMapper json=new ObjectMapper();
    final org.springframework.test.web.servlet.MockMvc mvc=MockMvcBuilders.standaloneSetup(new AdminController(service)).setControllerAdvice(new ApiExceptionHandler()).build();
    @Test void statusRejectsMissingNullCoercedFractionalNegativeAndAuthorityFields() throws Exception {
        for(String action:List.of("deactivate","reactivate"))for(String body:List.of("{}","{\"expectedVersion\":null}","{\"expectedVersion\":\"0\"}","{\"expectedVersion\":0.5}","{\"expectedVersion\":-1}","{\"expectedVersion\":0,\"role\":\"ADMIN\"}"))mvc.perform(patch("/api/admin/users/2/"+action).contentType("application/json").content(body)).andExpect(status().isBadRequest());verifyNoInteractions(service);
    }
    @Test void resetCannotOmitVersionOrAcceptPasswordHashAndProfileFields() throws Exception {for(String body:List.of("{\"newPassword\":\"Synthetic123\"}","{\"expectedVersion\":0,\"newPassword\":\"short\"}","{\"expectedVersion\":0,\"newPassword\":\"Synthetic123\",\"passwordHash\":\"hash\"}"))mvc.perform(post("/api/admin/users/2/reset-password").contentType("application/json").content(body)).andExpect(status().isBadRequest());verifyNoInteractions(service);}
    @Test void successDtoOnlyContainsIdentityRoleStateAndVersion() throws Exception {when(service.changeStatus(isNull(),eq(2L),any(),eq(false))).thenReturn(new AdminUserSummary("2","synthetic@example.test","Synthetic","USER",false,3));mvc.perform(patch("/api/admin/users/2/deactivate").contentType("application/json").content("{\"expectedVersion\":2}")).andExpect(status().isOk()).andExpect(header().string("Cache-Control","no-store")).andExpect(content().json("{\"id\":\"2\",\"email\":\"synthetic@example.test\",\"displayName\":\"Synthetic\",\"role\":\"USER\",\"enabled\":false,\"version\":3}",true));}
}
