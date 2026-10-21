package com.fitnutri.backend.activity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import java.time.Instant;
@Entity @Table(name="activity_dataset") @Getter @Setter @NoArgsConstructor
public class ActivityDataset {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @Column(nullable=false,length=128) private String name;
    @Column(name="release_name",nullable=false,length=128) private String release;
    @Column(nullable=false,length=128) private String revision;
    @Column(name="source_url",columnDefinition="text") private String sourceUrl;
    @Column(name="content_sha256",length=64) private String contentSha256;
    @Column(name="source_note",columnDefinition="text") private String sourceNote;
    @Column(name="min_age") private Integer minAge;
    @Column(name="max_age") private Integer maxAge;
    @Column(name="imported_at",nullable=false) private Instant importedAt=Instant.now();
    @Column(nullable=false) private boolean verified;
}
