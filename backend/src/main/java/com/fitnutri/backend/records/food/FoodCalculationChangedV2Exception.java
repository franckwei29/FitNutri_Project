package com.fitnutri.backend.records.food;
import com.fitnutri.backend.common.BusinessException;
import org.springframework.http.HttpStatus;
public final class FoodCalculationChangedV2Exception extends BusinessException {
 private final FoodEntryPreviewService.Response currentPreview;
 public FoodCalculationChangedV2Exception(FoodEntryPreviewService.Response currentPreview){super(HttpStatus.CONFLICT,"CALCULATION_CHANGED","The food calculation changed after review. Review the current preview before confirming again.");this.currentPreview=currentPreview;}
 public FoodEntryPreviewService.Response currentPreview(){return currentPreview;}
}
