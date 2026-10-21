package com.fitnutri.backend.submission;

import com.fitnutri.backend.user.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Entity @Table(name="submission_receipt") @Getter @Setter @NoArgsConstructor
public class SubmissionReceipt {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="user_id",nullable=false) private User user;
    @Column(name="submission_id",nullable=false) private UUID submissionId;
    @Enumerated(EnumType.STRING) @Column(nullable=false,length=20) private SubmissionOperation operation;
    @Column(name="request_sha256",nullable=false,length=64) private String requestSha256;
    @JdbcTypeCode(SqlTypes.JSON) @Column(name="result_json",columnDefinition="jsonb") private Map<String,Object> resultJson;
    @Column(name="created_at",nullable=false,updatable=false) private Instant createdAt=Instant.now();
    @Column(name="committed_at") private Instant committedAt;
}
