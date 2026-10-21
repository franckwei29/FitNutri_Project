package com.fitnutri.backend.records;
import com.fitnutri.backend.activity.ExerciseTypeResponse;
import java.math.BigDecimal;
import java.time.LocalDate;
public record ExerciseEstimateResponse(ExerciseTypeResponse exerciseType,LocalDate recordDate,BigDecimal durationMinutes,BigDecimal estimatedKj,EstimateStatus estimateStatus,String unavailableReason,BigDecimal weightKgUsed,LocalDate weightDateUsed,BigDecimal metUsed,String formulaVersion,String calculationFingerprint) {}
