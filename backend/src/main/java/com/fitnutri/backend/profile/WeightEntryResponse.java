package com.fitnutri.backend.profile;

import java.time.LocalDate;
import java.math.BigDecimal;

public record WeightEntryResponse(String id,LocalDate recordDate,BigDecimal weightKg,long version) {
    public static WeightEntryResponse from(WeightEntry e) {return new WeightEntryResponse(e.getId().toString(),e.getEntryDate(),e.getWeightKg(),e.getVersion());}
}
