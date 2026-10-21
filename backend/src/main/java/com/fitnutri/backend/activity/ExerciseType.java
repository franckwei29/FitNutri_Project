package com.fitnutri.backend.activity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
@Entity @Table(name="exercise_type") @Getter @Setter @NoArgsConstructor
public class ExerciseType {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="dataset_id",nullable=false) private ActivityDataset dataset;
    @Column(name="activity_key",nullable=false) private String activityKey;
    @Column(name="display_name",columnDefinition="text",nullable=false) private String displayName;
    @Column(name="source_activity_code") private String sourceActivityCode;
    @Column(name="source_description",columnDefinition="text") private String sourceDescription;
    @Column(name="met_value",precision=6,scale=2,nullable=false) private BigDecimal metValue;
    @Column(name="is_active",nullable=false) private boolean active;
}
