package com.fitnutri.backend.records.workout;
import com.fitnutri.backend.records.ExerciseRecord;
import com.fitnutri.backend.activity.catalogue.MovementCatalogue;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
@Entity @Table(name="workout_movement_log") @Getter @Setter
public class WorkoutMovementLog {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="exercise_record_id",nullable=false) private ExerciseRecord period;
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="movement_catalogue_id",nullable=false) private MovementCatalogue movement;
 @Column(nullable=false) private int position;
 @Column(name="name_snapshot",nullable=false,length=100) private String nameSnapshot;
 @Column(name="load_basis_snapshot",nullable=false,length=24) private String loadBasisSnapshot;
 @Column(name="set_count") private Integer setCount;
 private Integer repetitions;
 @Column(name="load_kg",precision=6,scale=2) private BigDecimal loadKg;
}
