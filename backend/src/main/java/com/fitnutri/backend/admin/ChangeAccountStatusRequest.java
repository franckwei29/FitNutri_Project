package com.fitnutri.backend.admin;
import com.fasterxml.jackson.annotation.*;
import com.fitnutri.backend.common.StrictVersionDeserializer;
import tools.jackson.databind.annotation.JsonDeserialize;
import jakarta.validation.constraints.*;
public record ChangeAccountStatusRequest(@JsonProperty(required=true) @NotNull @Min(0) @JsonDeserialize(using=StrictVersionDeserializer.class) Long expectedVersion) {
    @JsonAnySetter public void rejectUnknown(String name,Object value){throw new IllegalArgumentException("Unknown account status field");}
}
