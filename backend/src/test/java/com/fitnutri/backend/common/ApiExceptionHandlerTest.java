package com.fitnutri.backend.common;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.*;

class ApiExceptionHandlerTest {
    private MockMvc mvc;
    @BeforeEach void arrange() { mvc=MockMvcBuilders.standaloneSetup(new Probe()).setControllerAdvice(new ApiExceptionHandler()).build(); }
    @Test void businessFailureReturnsStableCodeAndFieldIdentity() throws Exception {
        mvc.perform(get("/probe/business")).andExpect(status().isConflict())
          .andExpect(jsonPath("$.code").value("VERSION_CONFLICT"))
          .andExpect(jsonPath("$.path").value("/probe/business"))
          .andExpect(jsonPath("$.requestId").value(matchesPattern("[0-9a-f-]{36}")))
          .andExpect(jsonPath("$.fieldErrors").isArray()).andExpect(jsonPath("$.retryable").value(false));
    }
    @Test void unexpectedExceptionsNeverExposeSqlSecretsOrStack() throws Exception {
        mvc.perform(get("/probe/crash")).andExpect(status().isInternalServerError())
          .andExpect(jsonPath("$.code").value("INTERNAL_ERROR"))
          .andExpect(content().string(not(containsString("password"))))
          .andExpect(content().string(not(containsString("SELECT"))))
          .andExpect(jsonPath("$.stackTrace").doesNotExist());
    }
    @Test void oldStatusExceptionsAreSanitised() throws Exception {
        mvc.perform(get("/probe/legacy")).andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
          .andExpect(content().string(not(containsString("secret"))));
    }
    @Test void beanValidationLocatesFieldWithoutRejectedValue() throws Exception {
        mvc.perform(post("/probe/body").contentType("application/json").content("{\"label\":\"\"}"))
          .andExpect(status().isBadRequest()).andExpect(jsonPath("$.fieldErrors[0].field").value("label"))
          .andExpect(jsonPath("$.fieldErrors[0].clientEntryId").value(nullValue()))
          .andExpect(jsonPath("$.fieldErrors[0].rejectedValue").doesNotExist());
    }
    @Test void malformedJsonAndInvalidIdsAre400Not500() throws Exception {
        mvc.perform(post("/probe/body").contentType("application/json").content("{")).andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
        mvc.perform(get("/probe/id/not-a-number")).andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }
    @Test void rateLimitedResponseHasBoundedRetryAfterAndSafeCanonicalBody() throws Exception {
        mvc.perform(get("/probe/rate-limit")).andExpect(status().isTooManyRequests()).andExpect(header().string("Retry-After","12"))
          .andExpect(header().string("Cache-Control","no-store")).andExpect(jsonPath("$.code").value("RATE_LIMITED"))
          .andExpect(jsonPath("$.retryable").value(true)).andExpect(jsonPath("$.requestId").value(matchesPattern("[0-9a-f-]{36}")));
    }
    @Test void servletUploadLimitIsAnExplicit413WithoutInternalExceptionDetails() throws Exception {
        mvc.perform(get("/probe/upload-limit")).andExpect(status().isPayloadTooLarge()).andExpect(jsonPath("$.code").value("PAYLOAD_TOO_LARGE")).andExpect(jsonPath("$.retryable").value(false)).andExpect(header().string("Cache-Control","no-store"));
    }
    @Test void mvcProtocolFailuresKeepTheirStatusesAndCanonicalSafeJson() throws Exception {
        mvc.perform(put("/probe/id/1")).andExpect(status().isMethodNotAllowed()).andExpect(jsonPath("$.code").value("REQUEST_REJECTED")).andExpect(header().string("Cache-Control","no-store"));
        mvc.perform(post("/probe/body").contentType("text/plain").content("synthetic-private")).andExpect(status().isUnsupportedMediaType()).andExpect(jsonPath("$.code").value("REQUEST_REJECTED")).andExpect(content().string(not(containsString("synthetic-private"))));
        mvc.perform(get("/probe/missing-resource")).andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("NOT_FOUND")).andExpect(content().string(not(containsString("private-resource"))));
    }
    @Test void databaseResourceAndTransactionStartFailuresAreSafeRetryable503()throws Exception{for(String path:new String[]{"/probe/database-down","/probe/transaction-down"})mvc.perform(get(path)).andExpect(status().isServiceUnavailable()).andExpect(jsonPath("$.code").value("DATABASE_UNAVAILABLE")).andExpect(jsonPath("$.retryable").value(true)).andExpect(content().string(not(containsString("private SQL")))).andExpect(header().string("Cache-Control","no-store"));}
    @Test void missingParametersAndUploadsIdentifyFieldsWithoutEchoingValues()throws Exception{
        mvc.perform(get("/probe/required")).andExpect(status().isBadRequest()).andExpect(jsonPath("$.fieldErrors[0].field").value("from"));
        mvc.perform(get("/probe/missing-part")).andExpect(status().isBadRequest()).andExpect(jsonPath("$.fieldErrors[0].field").value("audio"));
        mvc.perform(get("/probe/id/private-invalid-value")).andExpect(status().isBadRequest()).andExpect(jsonPath("$.fieldErrors[0].field").value("id")).andExpect(jsonPath("$.message").value(not(containsString("private-invalid-value")))).andExpect(jsonPath("$.fieldErrors[0].rejectedValue").doesNotExist());
    }
    @RestController static class Probe {
        @GetMapping("/probe/required") void required(@RequestParam String from){}
        @GetMapping("/probe/missing-part") void missingPart()throws Exception{throw new org.springframework.web.multipart.support.MissingServletRequestPartException("audio");}

        @GetMapping("/probe/database-down") void databaseDown(){throw new org.springframework.dao.DataAccessResourceFailureException("private SQL connection details");}
        @GetMapping("/probe/transaction-down") void transactionDown(){throw new org.springframework.transaction.CannotCreateTransactionException("private SQL transaction details");}
        record Body(@NotBlank String label) {}
        @GetMapping("/probe/missing-resource") void missing() throws Exception {throw new org.springframework.web.servlet.resource.NoResourceFoundException(org.springframework.http.HttpMethod.GET,"private-resource","/probe/missing-resource");}
        @GetMapping("/probe/upload-limit") void uploadLimit(){throw new org.springframework.web.multipart.MaxUploadSizeExceededException(10*1024*1024);}
        @GetMapping("/probe/rate-limit") void rateLimit(){throw new RateLimitedException(12);}
        @GetMapping("/probe/business") void business(){throw new BusinessException(HttpStatus.CONFLICT,"VERSION_CONFLICT","Reload and review the latest version.");}
        @GetMapping("/probe/crash") void crash(){throw new IllegalStateException("SELECT password FROM secret");}
        @GetMapping("/probe/legacy") void legacy(){throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"secret user input");}
        @PostMapping("/probe/body") void body(@Valid @RequestBody Body body) {}
        @GetMapping("/probe/id/{id}") void id(@PathVariable Long id) {}
    }
}
