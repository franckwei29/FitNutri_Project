package com.fitnutri.backend.qa;

import com.fasterxml.jackson.annotation.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.LocalDate;
public record QaRequest(@JsonProperty(required=true) @NotBlank @Size(max=2000) String message,@JsonProperty(required=true) @Valid RecordRange recordRange,@JsonProperty(required=true) @NotBlank String zone){
 @JsonAnySetter public void rejectUnknown(String name,Object value){throw new IllegalArgumentException("Unknown Q&A field");}
 public record RecordRange(@JsonProperty(required=true) @NotNull LocalDate from,@JsonProperty(required=true) @NotNull LocalDate to){@JsonAnySetter public void rejectUnknown(String name,Object value){throw new IllegalArgumentException("Unknown record range field");}}
}
