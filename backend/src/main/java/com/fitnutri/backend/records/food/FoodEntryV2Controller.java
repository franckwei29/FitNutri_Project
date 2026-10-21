package com.fitnutri.backend.records.food;
import com.fitnutri.backend.common.*;
import com.fitnutri.backend.submission.SubmissionReceiptV2Response;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/v2/food")
public class FoodEntryV2Controller {
 private final FoodEntryPreviewService previews;private final CurrentUserService current;private final FoodEntryV2Service entries;private final FoodEntryMutationService mutations;
 public FoodEntryV2Controller(FoodEntryPreviewService previews,CurrentUserService current,FoodEntryV2Service entries,FoodEntryMutationService mutations){this.previews=previews;this.current=current;this.entries=entries;this.mutations=mutations;}
 @PostMapping("/preview") public ResponseEntity<FoodEntryPreviewService.Response> preview(@RequestBody FoodEntryInputs.PreviewRequest request,Authentication auth){return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(previews.previewNewBatch(current.requireGeneralUser(auth).getId(),request));}
 @PostMapping("/entries/{kind}/{id}/preview") public ResponseEntity<FoodEntryPreviewService.Response> correction(@PathVariable String kind,@PathVariable Long id,@RequestBody FoodEntryInputs.CorrectionRequest request,Authentication auth){return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(previews.previewOwnedRoot(current.requireGeneralUser(auth).getId(),kind,id,request));}
 @PostMapping("/batches") public ResponseEntity<SubmissionReceiptV2Response> create(@RequestBody FoodEntryInputs.BatchRequest request,Authentication auth){var result=entries.createBatchV2(current.requireGeneralUser(auth).getId(),request);return ResponseEntity.status(result.replayed()?HttpStatus.OK:HttpStatus.CREATED).cacheControl(CacheControl.noStore()).header("Idempotent-Replay",Boolean.toString(result.replayed())).body(result);}
 @PutMapping("/entries/{kind}/{id}") public ResponseEntity<FoodSnapshotReader.Root> update(@PathVariable String kind,@PathVariable Long id,@RequestBody FoodEntryInputs.UpdateRequest request,Authentication auth){return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(mutations.updateOwnedRoot(current.requireGeneralUser(auth).getId(),kind,id,request));}
 @DeleteMapping("/entries/{kind}/{id}") public ResponseEntity<Void> delete(@PathVariable String kind,@PathVariable Long id,@RequestParam Long expectedVersion,Authentication auth){mutations.softDeleteOwnedRoot(current.requireGeneralUser(auth).getId(),kind,id,expectedVersion);return ResponseEntity.noContent().cacheControl(CacheControl.noStore()).build();}
 @ExceptionHandler(FoodCalculationChangedV2Exception.class) public ResponseEntity<FoodCalculationChangedV2Response> changed(FoodCalculationChangedV2Exception error,HttpServletRequest request){return ResponseEntity.status(HttpStatus.CONFLICT).cacheControl(CacheControl.noStore()).body(FoodCalculationChangedV2Response.from(ApiError.create(409,error.code(),error.getMessage(),request,List.of(),false),error.currentPreview()));}
}
