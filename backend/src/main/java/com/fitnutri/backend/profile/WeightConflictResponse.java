package com.fitnutri.backend.profile;

import com.fitnutri.backend.common.ApiError;
import com.fitnutri.backend.common.FieldViolation;
import java.util.List;

public record WeightConflictResponse(int status,String code,String message,String path,String requestId,List<FieldViolation> fieldErrors,boolean retryable,WeightEntryResponse currentItem) {
    public static WeightConflictResponse from(ApiError error,WeightEntryResponse current) {
        return new WeightConflictResponse(error.status(),error.code(),error.message(),error.path(),error.requestId(),error.fieldErrors(),error.retryable(),current);
    }
}
