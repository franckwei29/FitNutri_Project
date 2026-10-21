package com.fitnutri.backend.summary;

import java.util.List;

/** Retired legacy response; canonical handlers use SummaryModels only. */
@Deprecated
public record TrendsResponse(
        String rangeStart,
        String rangeEnd,
        int totalDays,
        List<DaySummary> days,
        int totalFoodRecordCount,
        int totalExerciseRecordCount,
        int daysWithFoodData,
        int daysWithExerciseData,
        double totalCaloriesConsumed,
        Double averageCaloriesConsumed,
        Double totalProteinGrams,
        Double averageProteinGrams,
        Double totalCarbsGrams,
        Double averageCarbsGrams,
        Double totalFatGrams,
        Double averageFatGrams,
        double totalCaloriesBurned,
        Double averageCaloriesBurned
) {
}