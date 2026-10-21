package com.fitnutri.backend.submission;
import com.fitnutri.backend.common.CurrentUserService;
import java.util.UUID;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/v2/submissions")
public class SubmissionV2Controller {
 private final SubmissionService service;private final CurrentUserService current;
 public SubmissionV2Controller(SubmissionService service,CurrentUserService current){this.service=service;this.current=current;}
 @GetMapping("/{submissionId}") public ResponseEntity<SubmissionReceiptV2Response> receipt(@PathVariable UUID submissionId,Authentication auth){return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(service.findReceiptV2(current.requireGeneralUser(auth).getId(),submissionId));}
}
