package com.fitnutri.backend.qa;

import com.fitnutri.backend.common.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/qa")
public class QaController {
 private final QaService qa;private final CurrentUserService current;
 public QaController(QaService qa,CurrentUserService current){this.qa=qa;this.current=current;}
 @PostMapping(value="/messages",consumes="application/json") public ResponseEntity<QaResponse> ask(@Valid @RequestBody QaRequest request,Authentication auth,HttpServletRequest http){return ResponseEntity.ok().header("Cache-Control","no-store").body(qa.ask(current.requireGeneralUser(auth),request).withRequestId(RequestCorrelation.id(http)));}
}
