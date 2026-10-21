package com.fitnutri.backend.records.workout;
import com.fitnutri.backend.user.User;
import com.fitnutri.backend.records.RecordSource;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.*;
import java.util.UUID;
/** Historical grouping only: no duplicate duration or energy ledger on this parent. */
@Entity @Table(name="workout_record") @Getter @Setter
public class WorkoutRecord {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="user_id",nullable=false) private User user;
 @Column(name="client_entry_id",nullable=false) private UUID clientEntryId;
 @Column(nullable=false,length=100) private String title="Gym workout";
 @Column(name="recorded_date",nullable=false) private LocalDate recordDate;
 @Enumerated(EnumType.STRING) @Column(nullable=false,length=16) private RecordSource source;
 @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="origin_workout_id") private WorkoutRecord origin;
 @Column(name="created_at",nullable=false,updatable=false) private Instant createdAt=Instant.now();
 @Column(name="updated_at",nullable=false) private Instant updatedAt=createdAt;
 @Version @Column(nullable=false) private long version;
 @Column(name="deleted_at") private Instant deletedAt;
}
