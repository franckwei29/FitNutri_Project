package com.fitnutri.backend.speech;

import com.fasterxml.jackson.annotation.*;
import jakarta.validation.constraints.*;
import java.time.LocalDate;
public record ParseSpeechRequest(@JsonProperty(required=true) @NotNull Kind kind,@JsonProperty(required=true) @NotBlank @Size(max=10000) String transcript,@JsonProperty(required=true) @NotNull LocalDate baseDate,@JsonProperty(required=true) @NotBlank String zone){
    public enum Kind {FOOD,EXERCISE}
    @JsonAnySetter public void rejectUnknown(String name,Object value){throw new IllegalArgumentException("Unknown speech field");}
}
