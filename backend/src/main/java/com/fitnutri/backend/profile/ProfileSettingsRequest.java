package com.fitnutri.backend.profile;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;

public record ProfileSettingsRequest(
        @JsonProperty(required=true) @NotNull @Min(0) Long expectedUserVersion,
        @JsonProperty(required=true) @Min(0) Long expectedProfileVersion,
        @JsonProperty(required=true) @NotBlank @Size(max=100) String displayName,
        @JsonProperty(required=true) @NotNull @Min(1900) Integer birthYear,
        @JsonProperty(required=true) @NotNull @AssertTrue Boolean adultConfirmed,
        @JsonProperty(required=true) @NotNull Sex sex,
        @JsonProperty(required=true) @NotNull @DecimalMin("50") @DecimalMax("260") BigDecimal heightCm,
        @JsonProperty(required=true) @NotNull ActivityLevel activityLevel,
        @JsonProperty(required=true) Goal goalType,
        @JsonProperty(required=true) @DecimalMin(value="0",inclusive=false) @DecimalMax("999999999.999") @Digits(integer=9,fraction=3) BigDecimal dailyTargetKj,
        @JsonProperty(required=true) @NotNull EnergyUnit energyUnit,
        @JsonProperty(required=true) @NotNull HealthSafetyAnswer healthSafetyAnswer,
        @JsonProperty(required=true) @NotNull Boolean qaConsentGranted,
        @JsonProperty(required=true) @NotBlank String zone
) {
    public ProfileSettingsRequest { displayName=displayName==null?null:displayName.trim(); }
    @JsonAnySetter public void rejectUnknown(String name,Object value) { throw new IllegalArgumentException("Unknown settings field"); }
}
