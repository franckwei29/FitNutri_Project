package com.fitnutri.backend.common;

import com.fitnutri.backend.user.User;
import com.fitnutri.backend.user.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Service
public class CurrentUserService {
    private final UserRepository users;
    public CurrentUserService(UserRepository users) { this.users=users; }
    public User requireActive(Authentication authentication) {
        if (authentication==null || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken
                || authentication.getName()==null || authentication.getName().isBlank()) throw unauthenticated();
        User user=users.findByEmail(authentication.getName()).orElseThrow(CurrentUserService::unauthenticated);
        if (!user.isEnabled()) throw unauthenticated();
        return user;
    }
    public User requireGeneralUser(Authentication authentication) {
        User user=requireActive(authentication);
        if (user.getRole()!=User.Role.USER) throw new BusinessException(HttpStatus.FORBIDDEN,"FORBIDDEN","This operation requires a user account.");
        return user;
    }
    private static BusinessException unauthenticated() {
        return new BusinessException(HttpStatus.UNAUTHORIZED,"UNAUTHENTICATED","Sign in to continue.");
    }
}
