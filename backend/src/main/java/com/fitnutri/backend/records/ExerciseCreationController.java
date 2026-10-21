package com.fitnutri.backend.records;
import com.fitnutri.backend.common.*;
import com.fitnutri.backend.submission.SubmissionReceiptResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/records/exercise")
public class ExerciseCreationController {
    private final ExerciseRecordService service;private final ExerciseEstimateService estimates;private final CurrentUserService current;private final OwnedPreviewService previews;
    public ExerciseCreationController(ExerciseRecordService service,ExerciseEstimateService estimates,CurrentUserService current,OwnedPreviewService previews){this.service=service;this.estimates=estimates;this.current=current;this.previews=previews;}
    @GetMapping("/estimate") public ExerciseEstimateResponse estimate(@RequestParam Long exerciseTypeId,@RequestParam BigDecimal durationMinutes,@RequestParam @DateTimeFormat(iso=DateTimeFormat.ISO.DATE)LocalDate recordDate,@RequestParam(required=false)String zone,@RequestParam(required=false)Long recordId,Authentication auth){Long owner=current.requireGeneralUser(auth).getId();return recordId==null?estimates.estimate(owner,exerciseTypeId,recordDate,durationMinutes,zone):previews.exercise(owner,exerciseTypeId,recordDate,durationMinutes,zone,recordId);}
    @PostMapping public ResponseEntity<SubmissionReceiptResponse> create(@Valid @RequestBody CreateExerciseRecordRequest request,Authentication auth){var receipt=service.confirmExercise(current.requireGeneralUser(auth).getId(),request);return ResponseEntity.status(receipt.replayed()?200:201).header("Cache-Control","no-store").header("Idempotent-Replay",Boolean.toString(receipt.replayed())).body(receipt);}
    @ExceptionHandler(CalculationChangedException.class) public ResponseEntity<CalculationChangedResponse> changed(CalculationChangedException error,HttpServletRequest request){var base=ApiError.create(409,error.code(),error.getMessage(),request,List.of(),false);return ResponseEntity.status(409).header("Cache-Control","no-store").body(CalculationChangedResponse.from(base,error.currentPreview()));}
}
