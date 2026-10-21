package com.fitnutri.backend.common;

import com.fitnutri.backend.user.User;
import com.fitnutri.backend.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import java.util.List;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CurrentUserServiceTest {
    private final UserRepository repository=mock(UserRepository.class);
    private final CurrentUserService service=new CurrentUserService(repository);
    private User user;
    private UsernamePasswordAuthenticationToken auth;
    @BeforeEach void arrange() {
        user=new User(); user.setId(41L);user.setEmail("fixture@example.test");
        auth=UsernamePasswordAuthenticationToken.authenticated(user.getEmail(),null,List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
        when(repository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
    }
    @Test void authenticatedOwnerComesFromDatabaseNotClientAuthorities() {
        assertEquals(41L, service.requireGeneralUser(auth).getId());
        assertEquals(User.Role.USER,service.requireActive(auth).getRole());
    }
    @Test void anonymousAndUnauthenticatedCannotResolveOwner() {
        assertEquals(401,assertThrows(BusinessException.class,()->service.requireActive(null)).status().value());
        assertThrows(BusinessException.class,()->service.requireActive(UsernamePasswordAuthenticationToken.unauthenticated(user.getEmail(),"ignored")));
        assertThrows(BusinessException.class,()->service.requireActive(new AnonymousAuthenticationToken("key","anonymousUser",List.of(new SimpleGrantedAuthority("ROLE_ANONYMOUS")))));
        verifyNoInteractions(repository);
    }
    @Test void missingAccountRejectsExistingSession() {
        when(repository.findByEmail(user.getEmail())).thenReturn(Optional.empty());
        assertEquals("UNAUTHENTICATED",assertThrows(BusinessException.class,()->service.requireActive(auth)).code());
    }
    @Test void disabledAccountRejectsExistingSession() {
        user.setEnabled(false);
        assertEquals(401,assertThrows(BusinessException.class,()->service.requireActive(auth)).status().value());
    }
    @Test void adminDoesNotAutomaticallyGainPrivateUserRecordScope() {
        user.setRole(User.Role.ADMIN);
        assertEquals("FORBIDDEN",assertThrows(BusinessException.class,()->service.requireGeneralUser(auth)).code());
        assertEquals(User.Role.ADMIN,service.requireActive(auth).getRole());
    }
}
