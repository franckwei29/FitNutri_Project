package com.fitnutri.backend.records.workout;
import com.fitnutri.backend.common.BusinessException;
import org.springframework.http.HttpStatus;
public final class ExerciseCalculationChangedV2Exception extends BusinessException{
 private final WorkoutEstimateService.Response currentPreview;
 public ExerciseCalculationChangedV2Exception(WorkoutEstimateService.Response preview){super(HttpStatus.CONFLICT,"CALCULATION_CHANGED","The exercise estimate changed after review. Review the current preview before confirming again.");this.currentPreview=preview;}
 public WorkoutEstimateService.Response currentPreview(){return currentPreview;}
}
