package com.fitnutri.backend.records.food;
import com.fitnutri.backend.common.*;
import java.util.List;
public record FoodCalculationChangedV2Response(int status,String code,String message,String path,String requestId,List<FieldViolation> fieldErrors,boolean retryable,FoodEntryPreviewService.Response currentPreview){
 public static FoodCalculationChangedV2Response from(ApiError base,FoodEntryPreviewService.Response preview){return new FoodCalculationChangedV2Response(base.status(),base.code(),base.message(),base.path(),base.requestId(),base.fieldErrors(),base.retryable(),preview);}
}
