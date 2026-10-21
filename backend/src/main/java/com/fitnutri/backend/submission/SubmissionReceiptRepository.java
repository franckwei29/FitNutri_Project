package com.fitnutri.backend.submission;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;
public interface SubmissionReceiptRepository extends JpaRepository<SubmissionReceipt,Long> {
    Optional<SubmissionReceipt> findByUserIdAndSubmissionId(Long userId,UUID submissionId);
}
