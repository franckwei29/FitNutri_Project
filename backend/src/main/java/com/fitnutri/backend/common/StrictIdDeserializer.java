package com.fitnutri.backend.common;
import tools.jackson.core.*;
import tools.jackson.databind.*;
/** IDs cross JSON boundaries as strings, including values above JavaScript's exact-integer limit. */
public final class StrictIdDeserializer extends ValueDeserializer<String> {
    @Override public String deserialize(JsonParser parser,DeserializationContext context){if(!parser.hasToken(JsonToken.VALUE_STRING))return context.reportInputMismatch(String.class,"Use a string identity.");return parser.getString();}
}
