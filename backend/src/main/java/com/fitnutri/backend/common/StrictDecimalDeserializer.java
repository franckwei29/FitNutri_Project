package com.fitnutri.backend.common;
import java.math.BigDecimal;
import tools.jackson.core.*;
import tools.jackson.databind.*;
/** Canonical quantities are JSON numbers, never strings, booleans or null coerced into zero. */
public final class StrictDecimalDeserializer extends ValueDeserializer<BigDecimal> {
    @Override public BigDecimal deserialize(JsonParser parser,DeserializationContext context){if(parser.currentToken()==null||!parser.currentToken().isNumeric())return context.reportInputMismatch(BigDecimal.class,"Use a decimal JSON number.");return parser.getDecimalValue();}
}
