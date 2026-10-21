package com.fitnutri.backend.records;
import com.fitnutri.backend.common.BusinessException;
import org.springframework.http.HttpStatus;
public class CalculationChangedException extends BusinessException {
    private final ExerciseEstimateResponse currentPreview;
    public CalculationChangedException(ExerciseEstimateResponse preview){super(HttpStatus.CONFLICT,"CALCULATION_CHANGED","The calculation basis changed after review. Review the current estimate before confirming again.");currentPreview=preview;}
    public ExerciseEstimateResponse currentPreview(){return currentPreview;}
}
