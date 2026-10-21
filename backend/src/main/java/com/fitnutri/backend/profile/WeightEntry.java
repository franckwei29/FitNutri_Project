package com.fitnutri.backend.profile;

import com.fitnutri.backend.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.math.BigDecimal;
import java.math.RoundingMode;
import jakarta.persistence.Version;
import java.time.LocalDate;

/** Dated weight measurement with an optimistic version for explicit corrections (FR06). */
@Entity
@Table(name = "weight_entry")
@Getter
@Setter
@NoArgsConstructor
public class WeightEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "weight_kg", nullable = false, precision = 6, scale = 2)
    private BigDecimal weightKg;

    @Column(name = "entry_date", nullable = false)
    private LocalDate entryDate;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Version
    @Column(nullable = false)
    private long version;
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = createdAt;

    public static WeightEntry of(User user, Double legacyWeightKg, LocalDate entryDate) {
        return of(user, BigDecimal.valueOf(legacyWeightKg), entryDate);
    }
    public static WeightEntry of(User user, BigDecimal weightKg, LocalDate entryDate) {
        WeightEntry e = new WeightEntry();
        e.setUser(user);
        e.setWeightKg(weightKg.setScale(2, RoundingMode.UNNECESSARY));
        e.setEntryDate(entryDate);
        return e;
    }
}