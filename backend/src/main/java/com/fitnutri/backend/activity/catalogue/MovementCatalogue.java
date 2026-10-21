package com.fitnutri.backend.activity.catalogue;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.util.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
/** Named strength movement, with no independent MET, duration or energy. */
@Entity @Table(name="movement_catalogue") @Getter @Setter
public class MovementCatalogue {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @Column(name="movement_key",nullable=false,length=100) private String movementKey;
 @Column(nullable=false,length=100) private String revision;
 @Column(nullable=false) private boolean active;
 @Column(name="display_name",nullable=false,length=100) private String displayName;
 @JdbcTypeCode(SqlTypes.JSON) @Column(columnDefinition="jsonb",nullable=false) private List<String> aliases=List.of();
 @Column(name="body_region",nullable=false,length=32) private String bodyRegion;
 @Column(name="equipment_type",nullable=false,length=48) private String equipmentType;
 @Column(name="load_basis",nullable=false,length=24) private String loadBasis;
 @JdbcTypeCode(SqlTypes.JSON) @Column(name="allowed_fields",columnDefinition="jsonb",nullable=false) private List<String> allowedFields=List.of("setCount","repetitions","loadKg");
 @Column(name="review_status",nullable=false,length=16) private String reviewStatus;
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="default_period_option_id",nullable=false) private ExerciseOption defaultPeriodOption;
}
