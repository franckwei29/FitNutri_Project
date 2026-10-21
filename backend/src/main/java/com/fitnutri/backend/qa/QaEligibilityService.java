package com.fitnutri.backend.qa;

import com.fitnutri.backend.common.BusinessException;
import com.fitnutri.backend.profile.*;
import com.fitnutri.backend.user.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;

/** Fresh short transaction on every call, including post-provider checks; never trusts the cached eligibility flag. */
@Service
public class QaEligibilityService {
 private final UserRepository users;private final ProfileService profiles;
 public QaEligibilityService(UserRepository users,ProfileService profiles){this.users=users;this.profiles=profiles;}
 @Transactional(readOnly=true,propagation=Propagation.REQUIRES_NEW,isolation=Isolation.READ_COMMITTED)
 public void requireEligibility(Long owner){
  if(owner==null||owner<1)throw unauthenticated();var user=users.findById(owner).orElseThrow(QaEligibilityService::unauthenticated);
  if(!user.isEnabled())throw unauthenticated();if(user.getRole()!=User.Role.USER)throw new BusinessException(HttpStatus.FORBIDDEN,"FORBIDDEN","Q&A requires a user account.");
  UserProfile profile;try{profile=profiles.requireCompletedAdultSetup(owner);}catch(BusinessException error){if(error.code().equals("ONBOARDING_REQUIRED"))throw restricted();throw error;}
  if(!QaEligibility.isEligible(profile))throw restricted();
 }
 private static BusinessException unauthenticated(){return new BusinessException(HttpStatus.UNAUTHORIZED,"UNAUTHENTICATED","Sign in to continue.");}
 private static BusinessException restricted(){return new BusinessException(HttpStatus.FORBIDDEN,"QA_RESTRICTED","General Health Q&A requires completed adult setup, a current eligible safety response and explicit consent. Update your profile to review eligibility. Recording and factual summaries remain available.");}
}
