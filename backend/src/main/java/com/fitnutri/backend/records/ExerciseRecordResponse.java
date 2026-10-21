package com.fitnutri.backend.records;
import com.fitnutri.backend.activity.ExerciseTypeResponse;
import java.math.BigDecimal;
import java.time.*;
import java.util.UUID;
public record ExerciseRecordResponse(String id,UUID clientEntryId,LocalDate recordDate,String source,long version,DataQuality dataQuality,Instant createdAt,Instant updatedAt,ExerciseTypeResponse exerciseType,String exerciseName,BigDecimal durationMinutes,BigDecimal estimatedKj,EstimateStatus estimateStatus,BigDecimal weightKgUsed,LocalDate weightDateUsed,BigDecimal metUsed,String formulaVersion){
    static ExerciseRecordResponse from(ExerciseRecord r){var type=r.getReference();return new ExerciseRecordResponse(r.getId().toString(),r.getClientEntryId(),r.getRecordDate(),r.getSource().canonical().name(),r.getVersion(),r.getDataQuality(),r.getCreatedAt(),r.getUpdatedAt(),type==null||!type.getDataset().isVerified()?null:ExerciseTypeResponse.from(type),r.getExerciseType(),r.getDurationMinutes(),r.getEstimatedKj(),r.getEstimateStatus(),r.getWeightKgUsed(),r.getWeightDateUsed(),r.getMetUsed(),r.getFormulaVersion());}
}
