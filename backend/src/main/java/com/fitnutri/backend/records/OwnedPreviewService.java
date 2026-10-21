package com.fitnutri.backend.records;
import com.fitnutri.backend.foodreference.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.springframework.stereotype.Service;
/** Optional record context grants only an authenticated owner's original immutable-reference exception. */
@Service
public class OwnedPreviewService {
    private final RecordReadService records;private final FoodReferenceService foods;private final FoodCalculationService calculator;private final ExerciseCatalog types;private final ExerciseEstimateService estimates;
    public OwnedPreviewService(RecordReadService records,FoodReferenceService foods,FoodCalculationService calculator,ExerciseCatalog types,ExerciseEstimateService estimates){this.records=records;this.foods=foods;this.calculator=calculator;this.types=types;this.estimates=estimates;}
    public FoodNutritionResponse food(Long owner,Long referenceId,BigDecimal grams,Long recordId){var record=records.getFood(owner,recordId);var reference=foods.requireImmutable(referenceId);return record.reference()!=null&&referenceId.toString().equals(record.reference().referenceId())?calculator.previewHistorical(reference,grams):calculator.preview(reference,grams);}
    public ExerciseEstimateResponse exercise(Long owner,Long typeId,LocalDate date,BigDecimal minutes,String zone,Long recordId){var record=records.getExercise(owner,recordId);return record.exerciseType()!=null&&typeId.toString().equals(record.exerciseType().id())?estimates.estimateOriginalReference(owner,types.requireImmutable(typeId),date,minutes,zone):estimates.estimate(owner,typeId,date,minutes,zone);}
}
