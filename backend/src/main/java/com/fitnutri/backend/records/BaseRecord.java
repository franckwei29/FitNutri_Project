package com.fitnutri.backend.records;

import com.fitnutri.backend.user.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@MappedSuperclass
@Getter
@Setter
public abstract class BaseRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RecordSource source;

    @Column(name = "recorded_at", nullable = false)
    private Instant recordedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();
    @Column(name="recorded_date") private LocalDate recordDate;
    @Column(name="updated_at",nullable=false) private Instant updatedAt=createdAt;
    @Version @Column(nullable=false) private long version;
    @Column(name="deleted_at") private Instant deletedAt;
    @Column(name="client_entry_id") private UUID clientEntryId;
    @Enumerated(EnumType.STRING) @Column(name="data_quality",nullable=false,length=24)
    private DataQuality dataQuality=DataQuality.LEGACY_UNVERIFIED;

}
