package com.fitnutri.backend.profile;

/**
 * Single source of truth for General Health Q&A eligibility (FR04).
 * Eligible only when the health-safety answer is NO AND explicit consent was given.
 * FR23 must call isEligible() server-side before doing anything else.
 */
public final class QaEligibility {

    private QaEligibility() {
    }

    public static boolean isEligible(UserProfile profile) {
        return profile != null
                && profile.getHealthSafetyAnswer() == HealthSafetyAnswer.NO
                && Boolean.TRUE.equals(profile.getHealthSafetyConsent());
    }

    public static String explanation(UserProfile profile) {
        if (isEligible(profile)) {
            return "General Health Q&A is available. It provides general information only, not medical advice.";
        }
        if (profile == null || profile.getHealthSafetyAnswer() == null) {
            return "Complete the health-safety question to find out whether General Health Q&A is available.";
        }
        if (profile.getHealthSafetyAnswer() == HealthSafetyAnswer.NO) {
            return "General Health Q&A needs your consent. Confirm consent in your profile to enable it.";
        }
        return "General Health Q&A is not available based on your health-safety response. "
                + "Recording, overview, history, trends and data summaries remain available. "
                + "For personal health advice, please consult a doctor or Accredited Practising Dietitian.";
    }
}
