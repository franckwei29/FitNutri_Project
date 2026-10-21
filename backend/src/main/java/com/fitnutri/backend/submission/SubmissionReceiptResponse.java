package com.fitnutri.backend.submission;
import java.time.*;
import java.util.*;
public record SubmissionReceiptResponse(UUID submissionId,SubmissionOperation kind,Instant committedAt,boolean replayed,List<RecordIdentity> records) {
    public SubmissionReceiptResponse{records=List.copyOf(records);}
    public record RecordIdentity(String id,UUID clientEntryId,LocalDate recordDate,long versionAtCreation) {}
}
