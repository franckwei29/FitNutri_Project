package com.fitnutri.backend.user;

import com.fitnutri.backend.common.BusinessException;
import jakarta.validation.Validation;
import java.time.*;
import java.util.Optional;
import org.junit.jupiter.api.*;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AuthServiceTest {
    private static final jakarta.validation.ValidatorFactory VALIDATION=Validation.buildDefaultValidatorFactory();
    private final UserRepository users=mock(UserRepository.class);
    private final PasswordEncoder encoder=mock(PasswordEncoder.class);
    private AuthService service;
    @BeforeEach void arrange() {
        service=new AuthService(users,encoder,VALIDATION.getValidator(),Clock.fixed(Instant.parse("2026-10-16T08:00:00Z"),ZoneOffset.UTC));
        when(encoder.encode(anyString())).thenReturn("synthetic-hash");
        when(users.saveAndFlush(any(User.class))).thenAnswer(call->{User u=call.getArgument(0);u.setId(9L);return u;});
    }
    @AfterAll static void closeValidator() {VALIDATION.close();}
    @Test void publicRegistrationAlwaysUsesCanonicalUserIdentityAndClock() {
        var response=service.register(new RegisterRequest("  I@EXAMPLE.TEST ","Synthetic123","  Synthetic  "));
        assertEquals("9",response.id());assertEquals("i@example.test",response.email());assertEquals("Synthetic",response.displayName());assertEquals("USER",response.role());assertTrue(response.enabled());assertEquals(0,response.version());
        var saved=org.mockito.ArgumentCaptor.forClass(User.class);verify(users).saveAndFlush(saved.capture());
        assertEquals("synthetic-hash",saved.getValue().getPasswordHash());assertEquals(Instant.parse("2026-10-16T08:00:00Z"),saved.getValue().getCreatedAt());
        verify(users,never()).count();
    }
    @Test void missingAndInvalidFieldsFailBeforeHashingOrPersistence() {
        assertThrows(BusinessException.class,()->service.register(null));
        assertThrows(BusinessException.class,()->service.register(new RegisterRequest("bad-address","short"," ")));
        verify(encoder,never()).encode(anyString());verify(users,never()).saveAndFlush(any());
    }
    @Test void byteBoundaryAccepts72ButNeverTruncates74BytePassword() {
        assertEquals("USER",service.register(new RegisterRequest("fixture@example.test","é".repeat(35)+"A1","Synthetic")).role());
        assertEquals("VALIDATION_ERROR",assertThrows(BusinessException.class,()->service.register(new RegisterRequest("other@example.test","é".repeat(36)+"A1","Synthetic"))).code());
        verify(encoder,times(1)).encode("é".repeat(35)+"A1");
    }
    @Test void duplicateEmailAndDatabaseRaceArePublicConflictsWithoutSqlLeaks() {
        User existing=new User();existing.setId(1L);when(users.findByCanonicalEmail("exists@example.test")).thenReturn(Optional.of(existing));
        assertEquals(409,assertThrows(BusinessException.class,()->service.register(new RegisterRequest("exists@example.test","Synthetic123","Synthetic"))).status().value());
        when(users.saveAndFlush(any())).thenThrow(new DataIntegrityViolationException("private SQL password"));
        var race=assertThrows(BusinessException.class,()->service.register(new RegisterRequest("race@example.test","Synthetic123","Synthetic")));
        assertEquals("EMAIL_ALREADY_REGISTERED",race.code());assertFalse(race.getMessage().contains("private"));
    }
    @Test void explicitBootstrapDoesNotPromoteOrResetExistingAccounts() {
        User user=new User();user.setId(1L);user.setEmail("existing@example.test");
        when(users.findByCanonicalEmail(user.getEmail())).thenReturn(Optional.of(user));
        assertThrows(BusinessException.class,()->service.bootstrapAdmin(new RegisterRequest(user.getEmail(),"Synthetic123","Synthetic")));
        assertEquals(User.Role.USER,user.getRole());
        user.setRole(User.Role.ADMIN);user.setPasswordHash("preserved-hash");
        assertEquals("1",service.bootstrapAdmin(new RegisterRequest(user.getEmail(),"Different123","Synthetic")).id());
        assertEquals("preserved-hash",user.getPasswordHash());verify(encoder,never()).encode(anyString());verify(users,never()).saveAndFlush(any());
    }
}
