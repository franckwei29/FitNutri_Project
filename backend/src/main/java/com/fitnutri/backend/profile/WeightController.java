package com.fitnutri.backend.profile;

import com.fitnutri.backend.common.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/profile/weight")
public class WeightController {
    private final WeightService weights;
    private final CurrentUserService current;
    public WeightController(WeightService weights,CurrentUserService current) {this.weights=weights;this.current=current;}
    @PostMapping public ResponseEntity<WeightService.WeightResult> upsert(@Valid @RequestBody AddWeightRequest request,Authentication auth) {
        var result=weights.upsertByDate(current.requireGeneralUser(auth).getId(),request);
        return ResponseEntity.status(result.created()?201:200).header("Cache-Control","no-store").body(result.result());
    }
    @GetMapping public List<WeightEntryResponse> history(@RequestParam LocalDate from,@RequestParam LocalDate to,@RequestParam(required=false) String zone,Authentication auth) {
        return weights.history(current.requireGeneralUser(auth).getId(),from,to,zone);
    }
    @ExceptionHandler(WeightConflictException.class)
    public ResponseEntity<WeightConflictResponse> conflict(WeightConflictException error,HttpServletRequest request) {
        var base=ApiError.create(409,error.code(),error.getMessage(),request,List.of(),false);
        return ResponseEntity.status(409).header("Cache-Control","no-store").body(WeightConflictResponse.from(base,error.currentItem()));
    }
}
