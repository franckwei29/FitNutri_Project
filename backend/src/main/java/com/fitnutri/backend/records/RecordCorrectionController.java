package com.fitnutri.backend.records;
import com.fitnutri.backend.common.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/records")
public class RecordCorrectionController {
    private final FoodRecordService foods;private final ExerciseRecordService exercises;private final CurrentUserService current;
    public RecordCorrectionController(FoodRecordService foods,ExerciseRecordService exercises,CurrentUserService current){this.foods=foods;this.exercises=exercises;this.current=current;}
    @PutMapping("/food/{id}") public FoodRecordResponse food(@PathVariable Long id,@Valid @RequestBody UpdateFoodRecordRequest request,Authentication auth){return foods.updateOwned(current.requireGeneralUser(auth).getId(),id,request);}
    @PutMapping("/exercise/{id}") public ExerciseRecordResponse exercise(@PathVariable Long id,@Valid @RequestBody UpdateExerciseRecordRequest request,Authentication auth){return exercises.updateOwned(current.requireGeneralUser(auth).getId(),id,request);}
    @DeleteMapping("/food/{id}") public ResponseEntity<Void> deleteFood(@PathVariable Long id,@RequestParam Long expectedVersion,Authentication auth){foods.softDeleteOwned(current.requireGeneralUser(auth).getId(),id,expectedVersion);return ResponseEntity.noContent().header("Cache-Control","no-store").build();}
    @DeleteMapping("/exercise/{id}") public ResponseEntity<Void> deleteExercise(@PathVariable Long id,@RequestParam Long expectedVersion,Authentication auth){exercises.softDeleteOwned(current.requireGeneralUser(auth).getId(),id,expectedVersion);return ResponseEntity.noContent().header("Cache-Control","no-store").build();}
    @ExceptionHandler(CalculationChangedException.class) public ResponseEntity<CalculationChangedResponse> changed(CalculationChangedException error,HttpServletRequest request){return ResponseEntity.status(409).header("Cache-Control","no-store").body(CalculationChangedResponse.from(ApiError.create(409,error.code(),error.getMessage(),request,List.of(),false),error.currentPreview()));}
}
