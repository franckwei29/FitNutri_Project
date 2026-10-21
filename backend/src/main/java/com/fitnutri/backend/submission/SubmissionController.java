package com.fitnutri.backend.submission;
import com.fitnutri.backend.common.CurrentUserService;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/submissions")
public class SubmissionController {
    private final SubmissionService submissions;private final CurrentUserService current;
    public SubmissionController(SubmissionService submissions,CurrentUserService current){this.submissions=submissions;this.current=current;}
    @GetMapping("/{submissionId}") public ResponseEntity<SubmissionReceiptResponse> receipt(@PathVariable UUID submissionId,Authentication auth){return ResponseEntity.ok().header("Cache-Control","no-store").body(submissions.findReceipt(current.requireGeneralUser(auth).getId(),submissionId));}
}
