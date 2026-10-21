package com.fitnutri.backend.profile;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.*;
import java.time.LocalDate;
import java.math.BigDecimal;

public record AddWeightRequest(
        @JsonProperty(required=true) @NotNull LocalDate recordDate,
        @JsonProperty(required=true) @NotNull @DecimalMin("20") @DecimalMax("400") @Digits(integer=3,fraction=2) BigDecimal weightKg,
        @JsonProperty(required=true) @Min(0) Long expectedVersion,
        @JsonProperty(required=true) @NotBlank String zone
) {
    @JsonAnySetter public void rejectUnknown(String name,Object value) {throw new IllegalArgumentException("Unknown weight field");}
}
