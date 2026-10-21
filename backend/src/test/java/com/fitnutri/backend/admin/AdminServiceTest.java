package com.fitnutri.backend.admin;

import com.fitnutri.backend.common.*;
import com.fitnutri.backend.user.*;
import jakarta.validation.Validation;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AdminServiceTest {
    final UserRepository users=mock(UserRepository.class);
    final CurrentUserService current=mock(CurrentUserService.class);
    final PasswordEncoder passwords=mock(PasswordEncoder.class);
    final Authentication auth=mock(Authentication.class);
    final Clock clock=Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"),ZoneOffset.UTC);
    final AdminService service=new AdminService(users,current,passwords,Validation.buildDefaultValidatorFactory().getValidator(),clock);
    User actor,target;
    @BeforeEach void arrange(){actor=user(1L,User.Role.ADMIN);target=user(2L,User.Role.USER);when(current.requireActive(auth)).thenReturn(actor);when(users.lockAdministrators()).thenReturn(List.of(actor));when(users.lockById(2L)).thenReturn(Optional.of(target));}
    private User user(Long id,User.Role role){User u=new User();u.setId(id);u.setRole(role);u.setEmail("synthetic-"+id+"@example.test");u.setDisplayName("Synthetic "+id);u.setPasswordHash("private-hash");return u;}
    @Test void disableLocksAdministratorsBeforeRecheckingActorAndTarget(){var result=service.changeStatus(auth,2L,new ChangeAccountStatusRequest(0L),false);assertFalse(result.enabled());assertEquals("2",result.id());assertEquals(clock.instant(),target.getUpdatedAt());var order=inOrder(users,current);order.verify(users).lockAdministrators();order.verify(current).requireActive(auth);order.verify(users).lockById(2L);order.verify(users).flush();}
    @Test void staleVersionCannotChangeStatus(){target.setVersion(2);assertEquals("VERSION_CONFLICT",assertThrows(BusinessException.class,()->service.changeStatus(auth,2L,new ChangeAccountStatusRequest(0L),false)).code());assertTrue(target.isEnabled());verify(users,never()).flush();}
    @Test void exactSameStateDoesNotFlushOrInventVersion(){assertTrue(service.changeStatus(auth,2L,new ChangeAccountStatusRequest(0L),true).enabled());assertEquals(0,target.getVersion());verify(users,never()).flush();}
    @Test void selfDisableAndLastAdministratorAreProtected(){when(users.lockById(1L)).thenReturn(Optional.of(actor));assertEquals("ACCOUNT_PROTECTED",assertThrows(BusinessException.class,()->service.changeStatus(auth,1L,new ChangeAccountStatusRequest(0L),false)).code());target.setRole(User.Role.ADMIN);actor.setEnabled(false);when(users.lockAdministrators()).thenReturn(List.of(actor,target));assertEquals("ACCOUNT_PROTECTED",assertThrows(BusinessException.class,()->service.changeStatus(auth,2L,new ChangeAccountStatusRequest(0L),false)).code());assertTrue(target.isEnabled());}
    @Test void userRoleAndMissingTargetCannotMutate(){actor.setRole(User.Role.USER);assertEquals("FORBIDDEN",assertThrows(BusinessException.class,()->service.changeStatus(auth,2L,new ChangeAccountStatusRequest(0L),false)).code());actor.setRole(User.Role.ADMIN);when(users.lockById(2L)).thenReturn(Optional.empty());assertEquals("NOT_FOUND",assertThrows(BusinessException.class,()->service.changeStatus(auth,2L,new ChangeAccountStatusRequest(0L),false)).code());verify(users,never()).flush();}
    @Test void missingNegativeAndNullVersionAreRejectedBeforeLocking(){for(var input:Arrays.asList(null,new ChangeAccountStatusRequest(null),new ChangeAccountStatusRequest(-1L)))assertThrows(BusinessException.class,()->service.changeStatus(auth,2L,input,false));verifyNoInteractions(users,current);}
    @Test void searchBoundsPreventUnboundedReads(){for(int[] bounds:List.of(new int[]{-1,20},new int[]{0,0},new int[]{0,101},new int[]{Integer.MAX_VALUE,100}))assertThrows(BusinessException.class,()->service.searchAccounts(auth,"",bounds[0],bounds[1]));assertThrows(BusinessException.class,()->service.searchAccounts(auth,"x".repeat(201),0,20));verifyNoInteractions(users);}
    @Test void passwordResetVersionsHashesAndRejectsOversizedUtf8BeforeEncoding(){when(passwords.encode("SyntheticNew123")).thenReturn("new-hash");service.resetPassword(auth,2L,new ResetPasswordRequest(0L,"SyntheticNew123"));assertEquals("new-hash",target.getPasswordHash());verify(users).flush();assertThrows(BusinessException.class,()->service.resetPassword(auth,2L,new ResetPasswordRequest(0L,"界".repeat(25)+"A1")));verify(passwords,times(1)).encode(anyString());}
}
