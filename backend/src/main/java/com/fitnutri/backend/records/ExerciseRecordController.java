package com.fitnutri.backend.records;
import com.fitnutri.backend.common.*;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/records/exercise")
public class ExerciseRecordController {
    private final RecordReadService records;private final CurrentUserService current;
    public ExerciseRecordController(RecordReadService records,CurrentUserService current){this.records=records;this.current=current;}
    @GetMapping public List<ExerciseRecordResponse> list(@RequestParam @DateTimeFormat(iso=DateTimeFormat.ISO.DATE)LocalDate from,@RequestParam @DateTimeFormat(iso=DateTimeFormat.ISO.DATE)LocalDate to,@RequestParam(required=false)Long exerciseTypeId,@RequestParam(required=false)String exerciseType,@RequestParam(required=false)String zone,Authentication auth){if(exerciseType!=null)throw BusinessException.invalid("exerciseType","Filter using an immutable exerciseTypeId.");return records.listExercises(current.requireGeneralUser(auth).getId(),from,to,exerciseTypeId,zone);}
    @GetMapping("/{id}") public ExerciseRecordResponse detail(@PathVariable Long id,Authentication auth){return records.getExercise(current.requireGeneralUser(auth).getId(),id);}
}
