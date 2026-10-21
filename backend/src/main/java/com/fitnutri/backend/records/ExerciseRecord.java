package com.fitnutri.backend.records;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import com.fitnutri.backend.activity.ExerciseType;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "exercise_record")
@Getter
@Setter
@NoArgsConstructor
public class ExerciseRecord extends BaseRecord {

    @Column(name = "exercise_type", nullable = false)
    private String exerciseType;

    @Column(name = "duration_minutes",nullable=false,precision=8,scale=2)
    private BigDecimal durationMinutes;

    @Enumerated(EnumType.STRING)
    // Legacy observations are preserved; canonical preset recording does not collect intensity.
    @Column
    private Intensity intensity;

    @Column(name = "estimated_calories")
    private Double estimatedCalories;
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="exercise_type_id") private ExerciseType reference;
    @Column(name="weight_kg_used",precision=6,scale=2) private BigDecimal weightKgUsed;
    @Column(name="weight_date_used") private LocalDate weightDateUsed;
    @Column(name="met_used",precision=6,scale=2) private BigDecimal metUsed;
    @Column(name="estimated_kj",precision=12,scale=3) private BigDecimal estimatedKj;
    @Enumerated(EnumType.STRING) @Column(name="estimate_status",nullable=false,length=32) private EstimateStatus estimateStatus=EstimateStatus.LEGACY_UNVERIFIED;
    @Column(name="formula_version",length=64) private String formulaVersion;
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="workout_id") private com.fitnutri.backend.records.workout.WorkoutRecord workout;
    @Column(name="period_position") private Integer periodPosition;
    @Column(name="period_kind",length=16) private String periodKind;
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="exercise_option_id") private com.fitnutri.backend.activity.catalogue.ExerciseOption option;
    @Column(name="option_name_snapshot",length=100) private String optionNameSnapshot;
    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.JSON) @Column(name="equipment_details",columnDefinition="jsonb",nullable=false) private java.util.Map<String,Object> equipmentDetails=java.util.Map.of();

}
