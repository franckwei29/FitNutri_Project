package com.fitnutri.backend.qa;

import com.fitnutri.backend.common.BusinessException;
import com.fitnutri.backend.profile.*;
import com.fitnutri.backend.user.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
class QaEligibilityServiceTest {
 final UserRepository users=mock(UserRepository.class);final ProfileService profiles=mock(ProfileService.class);final QaEligibilityService service=new QaEligibilityService(users,profiles);
 User user(){var user=new User();user.setId(1L);when(users.findById(1L)).thenReturn(Optional.of(user));return user;}
 UserProfile eligible(){user();var p=new UserProfile();p.setOnboardingCompleted(true);p.setAdultConfirmed(true);p.setHealthSafetyAnswer(HealthSafetyAnswer.NO);p.setHealthSafetyConsent(true);when(profiles.requireCompletedAdultSetup(1L)).thenReturn(p);return p;}
 @Test void freshSafetyAndConsentAreRequiredEvenWithAnOldEligibleCache(){var p=eligible();service.requireEligibility(1L);p.setAiQaEligible(true);p.setHealthSafetyConsent(false);assertEquals("QA_RESTRICTED",assertThrows(BusinessException.class,()->service.requireEligibility(1L)).code());p.setHealthSafetyConsent(true);p.setHealthSafetyAnswer(HealthSafetyAnswer.YES_OR_UNSURE);assertEquals(403,assertThrows(BusinessException.class,()->service.requireEligibility(1L)).status().value());verify(users,times(3)).findById(1L);verify(profiles,times(3)).requireCompletedAdultSetup(1L);}
 @Test void missingIncompleteSetupIsRestrictedRatherThanPretendingAnUnavailableModel(){user();when(profiles.requireCompletedAdultSetup(1L)).thenThrow(new BusinessException(org.springframework.http.HttpStatus.CONFLICT,"ONBOARDING_REQUIRED","Complete setup."));var error=assertThrows(BusinessException.class,()->service.requireEligibility(1L));assertEquals(403,error.status().value());assertEquals("QA_RESTRICTED",error.code());}
 @Test void inactiveMissingAndAdminScopeAreRecheckedBeforeProfile(){var u=user();u.setEnabled(false);assertEquals(401,assertThrows(BusinessException.class,()->service.requireEligibility(1L)).status().value());u.setEnabled(true);u.setRole(User.Role.ADMIN);assertEquals(403,assertThrows(BusinessException.class,()->service.requireEligibility(1L)).status().value());when(users.findById(1L)).thenReturn(Optional.empty());assertEquals(401,assertThrows(BusinessException.class,()->service.requireEligibility(1L)).status().value());verifyNoInteractions(profiles);}
}
