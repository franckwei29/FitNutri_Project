package com.fitnutri.backend.summary;

/** Retired legacy response; canonical handlers use SummaryModels only. */
@Deprecated
public record TodaySummaryResponse(
        String date,
        int foodRecordCount,
        double caloriesConsumed,
        Double proteinGrams,
        Double carbsGrams,
        Double fatGrams,
        int exerciseRecordCount,
        double caloriesBurned,
        Double calorieTarget,
        Double caloriesRemaining,
        String targetNote
) {
}