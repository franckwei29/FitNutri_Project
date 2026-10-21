package com.fitnutri.backend.summary;

import com.fitnutri.backend.profile.HealthSafetyAnswer;
import com.fitnutri.backend.profile.UserProfile;

import java.time.Year;

/** Legacy non-authoritative calculation retained for source history; no Spring bean or formal API consumer. */
@Deprecated
public class CalorieTargetCalculator {

    public record CalorieTarget(double bmr, double maintenanceCalories, Double targetCalories, String note) {
    }

    public CalorieTarget calculate(UserProfile profile) {
        if (profile.getBirthYear() == null || profile.getSex() == null
                || profile.getHeightCm() == null || profile.getWeightKg() == null
                || profile.getActivityLevel() == null) {
            return null;
        }

        int age = Year.now().getValue() - profile.getBirthYear();

        // Mifflin-St Jeor equation. There is no separately validated constant for
        // "other"/"prefer not to say"; we use the midpoint between the male and female
        // constants as a documented approximation rather than forcing a binary choice.
        double sexAdjustment = switch (profile.getSex()) {
            case MALE -> 5;
            case FEMALE -> -161;
            case OTHER, PREFER_NOT_TO_SAY -> -78;
        };

        double bmr = 10 * profile.getWeightKg() + 6.25 * profile.getHeightCm() - 5 * age + sexAdjustment;

        double activityFactor = switch (profile.getActivityLevel()) {
            case SEDENTARY -> 1.2;
            case LIGHT -> 1.375;
            case MODERATE -> 1.55;
            case HIGH -> 1.725;
        };

        double maintenance = bmr * activityFactor;

        // Safety boundary (matches the product design's onboarding rule): do not
        // auto-generate a deficit/surplus target for anyone who flagged a health concern,
        // is unsure, or declined to answer. They still see their maintenance estimate.
        if (profile.getHealthSafetyAnswer() == HealthSafetyAnswer.NO && profile.getGoal() == null) {
            return new CalorieTarget(bmr, maintenance, null,
                    "No goal is set, so your actual intake is shown without a goal comparison.");
        }
        if (profile.getHealthSafetyAnswer() != HealthSafetyAnswer.NO || profile.getGoal() == null) {
            return new CalorieTarget(bmr, maintenance, null,
                    "No automatic goal-based target is generated for this profile. "
                            + "Please consult a doctor or Accredited Practising Dietitian for personalised advice.");
        }

        Double target = switch (profile.getGoal()) {
            case LOSE_FAT -> Math.max(maintenance - 500, bmr);
            case GAIN_MUSCLE -> maintenance + 300;
            case MAINTAIN, NOT_SURE -> maintenance;
        };

        return new CalorieTarget(bmr, maintenance, target, null);
    }
}