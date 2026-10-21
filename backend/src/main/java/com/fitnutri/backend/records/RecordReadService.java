package com.fitnutri.backend.records;
import com.fitnutri.backend.common.*;
import java.time.LocalDate;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
@Service @Transactional(readOnly=true)
public class RecordReadService {
    private final FoodRecordRepository foods;private final ExerciseRecordRepository exercises;private final RecordDatePolicy dates;
    public RecordReadService(FoodRecordRepository foods,ExerciseRecordRepository exercises,RecordDatePolicy dates){this.foods=foods;this.exercises=exercises;this.dates=dates;}
    public List<FoodRecordResponse> listFoods(Long owner,LocalDate from,LocalDate to,MealType meal,String zone){dates.requireRange(from,to,zone);return foods.findOwnedRange(owner,from,to,meal).stream().map(FoodRecordResponse::from).toList();}
    public List<ExerciseRecordResponse> listExercises(Long owner,LocalDate from,LocalDate to,Long typeId,String zone){dates.requireRange(from,to,zone);if(typeId!=null)validId(typeId);return exercises.findOwnedRange(owner,from,to,typeId).stream().map(ExerciseRecordResponse::from).toList();}
    public FoodRecordResponse getFood(Long owner,Long id){validId(id);return FoodRecordResponse.from(foods.findOwnedActive(owner,id).orElseThrow(RecordReadService::missing));}
    public ExerciseRecordResponse getExercise(Long owner,Long id){validId(id);return ExerciseRecordResponse.from(exercises.findOwnedActive(owner,id).orElseThrow(RecordReadService::missing));}
    private static void validId(Long id){if(id==null||id<1)throw BusinessException.invalid("id","Choose a valid record or reference identity.");}
    private static BusinessException missing(){return new BusinessException(HttpStatus.NOT_FOUND,"NOT_FOUND","Record not found.");}
}
