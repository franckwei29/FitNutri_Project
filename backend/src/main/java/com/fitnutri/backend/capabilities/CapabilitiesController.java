package com.fitnutri.backend.capabilities;
import com.fitnutri.backend.common.CurrentUserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/capabilities")
public class CapabilitiesController {
    private final CapabilitiesService capabilities;private final CurrentUserService current;
    public CapabilitiesController(CapabilitiesService capabilities,CurrentUserService current){this.capabilities=capabilities;this.current=current;}
    @GetMapping public ResponseEntity<CapabilitiesResponse> get(Authentication auth){return ResponseEntity.ok().header("Cache-Control","no-store").body(capabilities.getCapabilities(current.requireActive(auth)));}
}
