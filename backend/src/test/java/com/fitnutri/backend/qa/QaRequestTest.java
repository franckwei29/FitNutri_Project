package com.fitnutri.backend.qa;

import jakarta.validation.Validation;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;
import static org.junit.jupiter.api.Assertions.*;
class QaRequestTest {
 final JsonMapper json=JsonMapper.builder().build();
 @Test void explicitNullableRangeAndStrictOwnerFreeFieldsAreEnforced(){var valid=json.readValue("{\"message\":\"General walking information\",\"recordRange\":null,\"zone\":\"Australia/Sydney\"}",QaRequest.class);assertNull(valid.recordRange());for(String body:new String[]{"{\"message\":\"walking\",\"zone\":\"Australia/Sydney\"}","{\"message\":\"walking\",\"recordRange\":null,\"zone\":\"Australia/Sydney\",\"userId\":\"999\"}","{\"message\":\"walking\",\"recordRange\":null,\"zone\":\"Australia/Sydney\",\"eligible\":true}","{\"message\":\"walking\",\"recordRange\":{\"from\":\"2020-01-01\",\"to\":\"2020-01-02\",\"userId\":\"999\"},\"zone\":\"Australia/Sydney\"}"})assertThrows(tools.jackson.core.JacksonException.class,()->json.readValue(body,QaRequest.class));var dated=json.readValue("{\"message\":\"Summarize my records\",\"recordRange\":{\"from\":\"2020-01-01\",\"to\":\"2020-01-02\"},\"zone\":\"Australia/Sydney\"}",QaRequest.class);assertEquals(java.time.LocalDate.of(2020,1,1),dated.recordRange().from());}
 @Test void messageAndNestedDateValidationDoNotAcceptMissingOrOverlongInput(){try(var factory=Validation.buildDefaultValidatorFactory()){var validator=factory.getValidator();assertTrue(validator.validate(new QaRequest("General information",null,"Australia/Sydney")).isEmpty());assertFalse(validator.validate(new QaRequest("x".repeat(2001),null,"Australia/Sydney")).isEmpty());assertFalse(validator.validate(new QaRequest("",null,"Australia/Sydney")).isEmpty());assertFalse(validator.validate(new QaRequest("records",new QaRequest.RecordRange(null,null),"Australia/Sydney")).isEmpty());}}
}
