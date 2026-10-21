package com.fitnutri.backend.records;
import com.fasterxml.jackson.annotation.*;
import com.fitnutri.backend.common.*;
import tools.jackson.databind.annotation.JsonDeserialize;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;
public record UpdateExerciseRecordRequest(
    @JsonProperty(required=true) @NotNull @Min(0) @JsonDeserialize(using=StrictVersionDeserializer.class) Long expectedVersion,
    @JsonProperty(required=true) @NotBlank @Pattern(regexp="[1-9][0-9]*") @Size(max=19) @JsonDeserialize(using=StrictIdDeserializer.class) String exerciseTypeId,
    @JsonProperty(required=true) @NotNull @DecimalMin(value="0",inclusive=false) @DecimalMax("1440") @JsonDeserialize(using=StrictDecimalDeserializer.class) BigDecimal durationMinutes,
    @JsonProperty(required=true) @NotNull LocalDate recordDate,
    @JsonProperty(required=true) @NotBlank String zone,
    @Size(min=1,max=128) String calculationFingerprint
) {@JsonAnySetter public void rejectUnknown(String name,Object value){throw new IllegalArgumentException("Unknown exercise correction field");}}
