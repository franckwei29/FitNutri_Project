package com.fitnutri.backend.common;
import jakarta.servlet.http.HttpServletRequest;
import java.util.UUID;
/** Server-generated only. Never trust a header or include user data in the correlation id. */
public final class RequestCorrelation {
    private static final String ATTRIBUTE=RequestCorrelation.class.getName()+".id";
    private RequestCorrelation(){}
    public static String id(HttpServletRequest request){Object value=request.getAttribute(ATTRIBUTE);if(value instanceof UUID id)return id.toString();UUID id=UUID.randomUUID();request.setAttribute(ATTRIBUTE,id);return id.toString();}
}
