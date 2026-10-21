package com.fitnutri.backend.records.workout;
import com.fitnutri.backend.common.*;
import java.util.List;
public record ExerciseCalculationChangedV2Response(int status,String code,String message,String path,String requestId,List<FieldViolation>fieldErrors,boolean retryable,WorkoutEstimateService.Response currentPreview){
 public static ExerciseCalculationChangedV2Response from(ApiError error,WorkoutEstimateService.Response preview){return new ExerciseCalculationChangedV2Response(error.status(),error.code(),error.message(),error.path(),error.requestId(),error.fieldErrors(),error.retryable(),preview);}
}
