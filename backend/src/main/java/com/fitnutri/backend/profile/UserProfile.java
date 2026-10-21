package com.fitnutri.backend.profile;

import com.fitnutri.backend.user.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.Instant;
import java.math.BigDecimal;

@Entity
@Table(name = "user_profile")
@Getter
@Setter
@NoArgsConstructor
public class UserProfile {

    @Id
    private Long userId;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "user_id")
    private User user;

    private Integer birthYear;

    @Enumerated(EnumType.STRING)
    private Sex sex;

    private Double heightCm;

    private Double weightKg;

    @Enumerated(EnumType.STRING)
    @Column(name = "activity_level")
    private ActivityLevel activityLevel;

    @Enumerated(EnumType.STRING)
    private Goal goal;

    @Enumerated(EnumType.STRING)
    @Column(name = "health_safety_answer")
    private HealthSafetyAnswer healthSafetyAnswer;

    @Column(name = "health_safety_consent")
    private Boolean healthSafetyConsent;

    @Column(name = "energy_goal_kcal")
    private Double energyGoalKcal;

    @Column(name = "ai_qa_eligible", nullable = false)
    private boolean aiQaEligible = false;

    @Column(name = "onboarding_completed", nullable = false)
    private boolean onboardingCompleted = false;

    @Version
    @Column(nullable = false)
    private long version;
    @Column(name = "adult_confirmed", nullable = false)
    private boolean adultConfirmed;
    @Enumerated(EnumType.STRING)
    @Column(name = "energy_unit", nullable = false, length = 4)
    private EnergyUnit energyUnit = EnergyUnit.KJ;
    @Column(name = "daily_target_kj", precision = 12, scale = 3)
    private BigDecimal dailyTargetKj;
    @Column(name = "health_safety_updated_at")
    private Instant healthSafetyUpdatedAt;
    @Column(name = "qa_consent_updated_at")
    private Instant qaConsentUpdatedAt;
    @Column(name = "updated_at")
    private Instant updatedAt;
}
