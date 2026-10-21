package com.fitnutri.backend.common;
import tools.jackson.core.*;
import tools.jackson.databind.*;
/** No string/boolean coercion or truncation of fractional optimistic versions. */
public final class StrictVersionDeserializer extends ValueDeserializer<Long> {
    @Override public Long deserialize(JsonParser parser,DeserializationContext context){
        if(parser.currentToken()==null||!parser.currentToken().isNumeric())return context.reportInputMismatch(Long.class,"Use an integer record version.");
        try{return parser.getDecimalValue().longValueExact();}catch(ArithmeticException e){return context.reportInputMismatch(Long.class,"Use an integer record version.");}
    }
}
