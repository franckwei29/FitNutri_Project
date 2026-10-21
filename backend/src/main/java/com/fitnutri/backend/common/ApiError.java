package com.fitnutri.backend.common;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;

public record ApiError(int status, String code, String message, String path,
                       String requestId, List<FieldViolation> fieldErrors, boolean retryable) {
    public static ApiError create(int status, String code, String message, HttpServletRequest request,
                                  List<FieldViolation> fields, boolean retryable) {
        String requestId=RequestCorrelation.id(request);
        return new ApiError(status,code,message,request.getRequestURI(),requestId,List.copyOf(fields),retryable);
    }
}
