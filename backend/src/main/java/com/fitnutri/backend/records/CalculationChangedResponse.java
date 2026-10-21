package com.fitnutri.backend.records;
import com.fitnutri.backend.common.*;
import java.util.List;
public record CalculationChangedResponse(int status,String code,String message,String path,String requestId,List<FieldViolation> fieldErrors,boolean retryable,ExerciseEstimateResponse currentPreview){
    public static CalculationChangedResponse from(ApiError e,ExerciseEstimateResponse preview){return new CalculationChangedResponse(e.status(),e.code(),e.message(),e.path(),e.requestId(),e.fieldErrors(),e.retryable(),preview);}
}
