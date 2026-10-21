package com.fitnutri.backend.records;
import com.fitnutri.backend.activity.*;
import com.fitnutri.backend.common.BusinessException;
import com.fitnutri.backend.foodreference.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
class OwnedPreviewServiceTest {
    private final RecordReadService reads=mock(RecordReadService.class);private final FoodReferenceService foods=mock(FoodReferenceService.class);private final FoodCalculationService calc=mock(FoodCalculationService.class);private final ExerciseCatalog types=mock(ExerciseCatalog.class);private final ExerciseEstimateService estimates=mock(ExerciseEstimateService.class);
    private final OwnedPreviewService service=new OwnedPreviewService(reads,foods,calc,types,estimates);
    @Test void onlyOwningOriginalFoodReferenceGetsHistoricalCalculationException(){var record=mock(FoodRecordResponse.class);when(record.reference()).thenReturn(new ReferenceInfo("101","synthetic","Synthetic",null,false));when(reads.getFood(1L,10L)).thenReturn(record);var ref=mock(FoodReference.class);when(foods.requireImmutable(101L)).thenReturn(ref);service.food(1L,101L,BigDecimal.TEN,10L);verify(calc).previewHistorical(ref,BigDecimal.TEN);verify(calc,never()).preview(any(),any());}
    @Test void differentFoodSelectionStillRequiresTheNormalActiveReferencePath(){var record=mock(FoodRecordResponse.class);when(record.reference()).thenReturn(new ReferenceInfo("101","synthetic","Synthetic",null,false));when(reads.getFood(1L,10L)).thenReturn(record);var ref=mock(FoodReference.class);when(foods.requireImmutable(102L)).thenReturn(ref);service.food(1L,102L,BigDecimal.TEN,10L);verify(calc).preview(ref,BigDecimal.TEN);verify(calc,never()).previewHistorical(any(),any());}
    @Test void foreignRecordCannotAuthorizeHistoricalFoodOrExercisePreview(){when(reads.getFood(2L,10L)).thenThrow(new BusinessException(HttpStatus.NOT_FOUND,"NOT_FOUND","Record not found."));when(reads.getExercise(2L,10L)).thenThrow(new BusinessException(HttpStatus.NOT_FOUND,"NOT_FOUND","Record not found."));assertThrows(BusinessException.class,()->service.food(2L,101L,BigDecimal.TEN,10L));assertThrows(BusinessException.class,()->service.exercise(2L,11L,LocalDate.of(2020,1,1),BigDecimal.TEN,"Australia/Sydney",10L));verifyNoInteractions(foods,calc,types,estimates);}
    @Test void owningOriginalExerciseAllowsHistoricalInputsButDifferentTypeUsesNormalEstimate(){var record=mock(ExerciseRecordResponse.class);var dto=mock(ExerciseTypeResponse.class);when(dto.id()).thenReturn("11");when(record.exerciseType()).thenReturn(dto);when(reads.getExercise(1L,10L)).thenReturn(record);var type=mock(ExerciseType.class);when(types.requireImmutable(11L)).thenReturn(type);var day=LocalDate.of(2020,1,1);service.exercise(1L,11L,day,BigDecimal.TEN,"Australia/Sydney",10L);verify(estimates).estimateOriginalReference(1L,type,day,BigDecimal.TEN,"Australia/Sydney");service.exercise(1L,12L,day,BigDecimal.TEN,"Australia/Sydney",10L);verify(estimates).estimate(1L,12L,day,BigDecimal.TEN,"Australia/Sydney");}
}
