package com.fitnutri.backend.user;

import com.fitnutri.backend.common.BusinessException;
import com.fitnutri.backend.common.CurrentUserService;
import jakarta.servlet.http.*;
import jakarta.validation.Valid;
import java.nio.charset.StandardCharsets;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.security.authentication.*;
import org.springframework.security.core.*;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;
    private final CurrentUserService currentUsers;
    private final AuthenticationManager authentications;
    private final SessionAuthenticationStrategy sessions;
    private final SecurityContextRepository contexts;
    private final LogoutHandler apiLogoutHandler;
    @PostMapping("/register") public ResponseEntity<UserResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).header("Cache-Control","no-store").body(authService.register(request));
    }
    @PostMapping("/login") public ResponseEntity<UserResponse> login(@Valid @RequestBody LoginRequest request,HttpServletRequest req,HttpServletResponse res) {
        if(request==null || request.password().getBytes(StandardCharsets.UTF_8).length>72) throw invalidCredentials();
        Authentication authentication;
        try {authentication=authentications.authenticate(UsernamePasswordAuthenticationToken.unauthenticated(request.email(),request.password()));}
        catch(AuthenticationException e) {throw invalidCredentials();}
        var user=currentUsers.requireActive(authentication);
        sessions.onAuthentication(authentication,req,res);
        var context=SecurityContextHolder.createEmptyContext();context.setAuthentication(authentication);SecurityContextHolder.setContext(context);contexts.saveContext(context,req,res);
        return ResponseEntity.ok().header("Cache-Control","no-store").body(UserResponse.from(user));
    }
    @PostMapping("/logout") public ResponseEntity<Void> logout(HttpServletRequest req,HttpServletResponse res) {
        apiLogoutHandler.logout(req,res,SecurityContextHolder.getContext().getAuthentication());
        return ResponseEntity.noContent().header("Cache-Control","no-store").build();
    }
    @GetMapping("/me") public ResponseEntity<UserResponse> me(Authentication authentication) {
        return ResponseEntity.ok().header("Cache-Control","no-store").body(UserResponse.from(currentUsers.requireActive(authentication)));
    }
    private static BusinessException invalidCredentials() {return new BusinessException(HttpStatus.UNAUTHORIZED,"UNAUTHENTICATED","Invalid email or password.");}
}
