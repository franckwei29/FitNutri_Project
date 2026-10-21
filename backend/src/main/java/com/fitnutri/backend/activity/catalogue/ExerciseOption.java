package com.fitnutri.backend.activity.catalogue;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.util.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
/** Versioned product choice; energy remains authoritative in ExerciseType. */
@Entity @Table(name="exercise_option") @Getter @Setter
public class ExerciseOption {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @Column(name="option_key",nullable=false,length=100) private String optionKey;
 @Column(nullable=false,length=100) private String revision;
 @Column(nullable=false) private boolean active;
 @Column(nullable=false,length=24) private String category;
 @Column(name="period_kind",nullable=false,length=16) private String periodKind;
 @Column(name="display_name",nullable=false,length=100) private String displayName;
 @JdbcTypeCode(SqlTypes.JSON) @Column(columnDefinition="jsonb",nullable=false) private List<String> aliases=List.of();
 @JdbcTypeCode(SqlTypes.JSON) @Column(name="allowed_details",columnDefinition="jsonb",nullable=false) private List<String> allowedDetails=List.of();
 @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="reference_id") private com.fitnutri.backend.activity.ExerciseType reference;
 @Column(name="mapping_kind",nullable=false,length=32) private String mappingKind;
 @JdbcTypeCode(SqlTypes.JSON) @Column(name="mapping_evidence",columnDefinition="jsonb",nullable=false) private Map<String,String> mappingEvidence=Map.of();
 @Column(name="review_status",nullable=false,length=16) private String reviewStatus;
}
