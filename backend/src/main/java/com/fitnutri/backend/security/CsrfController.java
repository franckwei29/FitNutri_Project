package com.fitnutri.backend.security;
import org.springframework.http.ResponseEntity;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.*;
@RestController
@RequestMapping("/api/auth")
public class CsrfController {
    public record CsrfResponse(String headerName,String token) {}
    @GetMapping("/csrf") public ResponseEntity<CsrfResponse> token(CsrfToken token) {
        return ResponseEntity.ok().header("Cache-Control","no-store").body(new CsrfResponse(token.getHeaderName(),token.getToken()));
    }
}
