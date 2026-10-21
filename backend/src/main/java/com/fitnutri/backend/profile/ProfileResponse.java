package com.fitnutri.backend.profile;

import com.fitnutri.backend.user.UserResponse;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/** Canonical mvp-1 response; never serializes a persistence entity or a password hash. */
public record ProfileResponse(
        Long profileVersion, UserResponse user, Integer birthYear, boolean adultConfirmed,
        Sex sex, Double heightCm, ActivityLevel activityLevel, Goal goalType,
        BigDecimal dailyTargetKj, EnergyUnit energyUnit, HealthSafetyAnswer healthSafetyAnswer,
        boolean qaConsentGranted, Eligibility qaEligibility, boolean onboardingCompleted,
        CurrentWeight currentWeight, Instant healthSafetyUpdatedAt, Instant qaConsentUpdatedAt, Instant updatedAt
) {
    public record Eligibility(boolean eligible,String reason) {}
    public record CurrentWeight(String id,LocalDate recordDate,BigDecimal weightKg,long version) {
        static CurrentWeight from(WeightEntry weight) {
            return weight==null?null:new CurrentWeight(weight.getId().toString(),weight.getEntryDate(),weight.getWeightKg(),weight.getVersion());
        }
    }
    static ProfileResponse from(com.fitnutri.backend.user.User user,UserProfile p,WeightEntry weight) {
        var eligibility=new Eligibility(QaEligibility.isEligible(p),QaEligibility.explanation(p));
        if(p==null) return new ProfileResponse(null,UserResponse.from(user),null,false,null,null,null,null,null,EnergyUnit.KJ,null,false,eligibility,false,CurrentWeight.from(weight),null,null,null);
        return new ProfileResponse(p.getVersion(),UserResponse.from(user),p.getBirthYear(),p.isAdultConfirmed(),p.getSex(),p.getHeightCm(),p.getActivityLevel(),p.getGoal(),p.getDailyTargetKj(),p.getEnergyUnit(),p.getHealthSafetyAnswer(),Boolean.TRUE.equals(p.getHealthSafetyConsent()),eligibility,p.isOnboardingCompleted(),CurrentWeight.from(weight),p.getHealthSafetyUpdatedAt(),p.getQaConsentUpdatedAt(),p.getUpdatedAt());
    }
}
