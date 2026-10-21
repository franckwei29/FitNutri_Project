package com.fitnutri.backend.records;
import com.fitnutri.backend.common.CurrentUserService;
import com.fitnutri.backend.submission.SubmissionReceiptResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/records/food")
public class FoodCreationController {
    private final FoodRecordService service;private final CurrentUserService current;
    public FoodCreationController(FoodRecordService service,CurrentUserService current){this.service=service;this.current=current;}
    @PostMapping("/batch") public ResponseEntity<SubmissionReceiptResponse> batch(@Valid @RequestBody FoodBatchRequest request,Authentication auth){return result(service.confirmBatch(current.requireGeneralUser(auth).getId(),request));}
    @PostMapping public ResponseEntity<SubmissionReceiptResponse> single(@Valid @RequestBody CreateFoodRecordRequest request,Authentication auth){return result(service.confirmSingleAdapter(current.requireGeneralUser(auth).getId(),request));}
    private static ResponseEntity<SubmissionReceiptResponse> result(SubmissionReceiptResponse r){return ResponseEntity.status(r.replayed()?200:201).header("Cache-Control","no-store").header("Idempotent-Replay",Boolean.toString(r.replayed())).body(r);}
}
