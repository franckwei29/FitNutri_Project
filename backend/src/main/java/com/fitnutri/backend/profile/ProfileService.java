package com.fitnutri.backend.profile;

import com.fitnutri.backend.common.*;
import com.fitnutri.backend.user.*;
import jakarta.validation.Validator;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.util.Objects;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProfileService {
    private final UserRepository users;
    private final UserProfileRepository profiles;
    private final WeightEntryRepository weights;
    private final RecordDatePolicy dates;
    private final Validator validator;
    private final Clock clock;
    public ProfileService(UserRepository users,UserProfileRepository profiles,WeightEntryRepository weights,RecordDatePolicy dates,Validator validator,Clock clock) {
        this.users=users;this.profiles=profiles;this.weights=weights;this.dates=dates;this.validator=validator;this.clock=clock;
    }
    @Transactional(readOnly=true)
    public ProfileResponse getCurrent(Long userId) {
        User user=activeUser(users.findById(userId).orElseThrow(ProfileService::unauthenticated));
        return response(user,profiles.findById(userId).orElse(null));
    }
    @Transactional
    public ProfileResponse completeOnboarding(Long userId,CompleteOnboardingRequest request) {
        validate(request);validateSettings(request.settings());
        dates.requireDate(request.initialWeight().recordDate(),request.zone());
        User user=lockUser(userId);
        UserProfile profile=profiles.findById(userId).orElse(null);
        if(profile!=null && profile.isOnboardingCompleted()) throw conflict("ONBOARDING_ALREADY_COMPLETED","Setup is already complete. Reload the current profile.");
        requireVersions(user,profile,request.expectedUserVersion(),request.expectedProfileVersion());
        boolean creating=profile==null;
        if(creating) { profile=new UserProfile();profile.setUser(user); }
        applySettings(user,profile,request.settings(),creating);
        // Persist the settings before weight to prove that later failures roll the whole transaction back.
        users.flush();profiles.saveAndFlush(profile);
        var weight=WeightEntry.of(user,request.initialWeight().weightKg(),request.initialWeight().recordDate());
        weight.setCreatedAt(clock.instant());weight.setUpdatedAt(clock.instant());weights.saveAndFlush(weight);
        profile.setWeightKg(weights.findFirstByUserIdOrderByEntryDateDesc(userId).orElseThrow().getWeightKg().doubleValue());
        profile.setOnboardingCompleted(true);profiles.flush();
        return response(user,profile);
    }
    @Transactional
    public ProfileResponse updateSettings(Long userId,ProfileSettingsRequest request) {
        validateSettings(request);
        User user=lockUser(userId);UserProfile profile=profiles.findById(userId).orElse(null);
        requireVersions(user,profile,request.expectedUserVersion(),request.expectedProfileVersion());
        if(profile==null || !profile.isOnboardingCompleted()) throw conflict("ONBOARDING_REQUIRED","Complete setup with an initial weight first.");
        applySettings(user,profile,request,false);
        users.flush();profiles.flush();return response(user,profile);
    }
    @Transactional
    public ProfileResponse clearGoal(Long userId,long expectedUserVersion,long expectedProfileVersion,boolean energy) {
        if(expectedUserVersion<0 || expectedProfileVersion<0) throw BusinessException.invalid("expectedProfileVersion","Use the current version.");
        User user=lockUser(userId);UserProfile profile=profiles.findById(userId).orElse(null);
        requireVersions(user,profile,expectedUserVersion,expectedProfileVersion);
        if(profile==null) throw conflict("ONBOARDING_REQUIRED","Complete setup first.");
        if(energy) {profile.setDailyTargetKj(null);profile.setEnergyGoalKcal(null);} else profile.setGoal(null);
        profile.setUpdatedAt(clock.instant());profiles.flush();return response(user,profile);
    }
    @Transactional(readOnly=true)
    public UserProfile requireCompletedAdultSetup(Long userId) {
        UserProfile profile=profiles.findById(userId).orElse(null);
        if(profile==null || !profile.isOnboardingCompleted() || !profile.isAdultConfirmed()) throw conflict("ONBOARDING_REQUIRED","Complete adult setup before using this feature.");
        return profile;
    }
    private void applySettings(User user,UserProfile p,ProfileSettingsRequest r,boolean creating) {
        if(!Objects.equals(user.getDisplayName(),r.displayName())) {user.setDisplayName(r.displayName());user.setUpdatedAt(clock.instant());}
        if(creating || p.getHealthSafetyAnswer()!=r.healthSafetyAnswer()) p.setHealthSafetyUpdatedAt(clock.instant());
        if(creating || !Objects.equals(p.getHealthSafetyConsent(),r.qaConsentGranted())) p.setQaConsentUpdatedAt(clock.instant());
        p.setBirthYear(r.birthYear());p.setAdultConfirmed(r.adultConfirmed());p.setSex(r.sex());p.setHeightCm(r.heightCm().doubleValue());
        p.setActivityLevel(r.activityLevel());p.setGoal(r.goalType());p.setDailyTargetKj(r.dailyTargetKj()==null?null:r.dailyTargetKj().setScale(3,RoundingMode.UNNECESSARY));
        p.setEnergyUnit(r.energyUnit());p.setHealthSafetyAnswer(r.healthSafetyAnswer());p.setHealthSafetyConsent(r.qaConsentGranted());
        // Transitional legacy cache only; canonical APIs and summaries read dailyTargetKj.
        p.setEnergyGoalKcal(r.dailyTargetKj()==null?null:r.dailyTargetKj().divide(new BigDecimal("4.184"),12,RoundingMode.HALF_UP).doubleValue());
        p.setAiQaEligible(QaEligibility.isEligible(p));p.setUpdatedAt(clock.instant());
    }
    private void validateSettings(ProfileSettingsRequest r) {
        validate(r);
        if(r.birthYear()>dates.today(r.zone()).getYear()-18) throw BusinessException.invalid("birthYear","FitNutri currently requires an adult birth year.");
        if(!Double.isFinite(r.heightCm().doubleValue())) throw BusinessException.invalid("heightCm","Enter a finite height.");
    }
    private void validate(Object request) {
        if(request==null) throw BusinessException.invalid("body","Enter the required settings.");
        var errors=validator.validate(request);
        if(!errors.isEmpty()) throw new BusinessException(HttpStatus.BAD_REQUEST,"VALIDATION_ERROR","Check the highlighted fields.",errors.stream().map(e->new FieldViolation(e.getPropertyPath().toString(),null,"Enter a valid value for this field.")).toList());
    }
    private User lockUser(Long id) {return activeUser(users.lockById(id).orElseThrow(ProfileService::unauthenticated));}
    private static User activeUser(User user) {
        if(!user.isEnabled()) throw unauthenticated();
        if(user.getRole()!=User.Role.USER) throw new BusinessException(HttpStatus.FORBIDDEN,"FORBIDDEN","This operation requires a user account.");
        return user;
    }
    private static void requireVersions(User u,UserProfile p,Long userVersion,Long profileVersion) {
        if(!Objects.equals(userVersion,u.getVersion()) || !Objects.equals(profileVersion,p==null?null:p.getVersion())) throw conflict("VERSION_CONFLICT","Reload and review the latest version.");
    }
    private ProfileResponse response(User user,UserProfile p) {return ProfileResponse.from(user,p,weights.findFirstByUserIdOrderByEntryDateDesc(user.getId()).orElse(null));}
    private static BusinessException conflict(String code,String message) {return new BusinessException(HttpStatus.CONFLICT,code,message);}
    private static BusinessException unauthenticated() {return new BusinessException(HttpStatus.UNAUTHORIZED,"UNAUTHENTICATED","Sign in to continue.");}
}
