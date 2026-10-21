package com.fitnutri.backend.admin;
import com.fasterxml.jackson.annotation.*;
import com.fitnutri.backend.common.StrictVersionDeserializer;
import tools.jackson.databind.annotation.JsonDeserialize;
import jakarta.validation.constraints.*;
public record ResetPasswordRequest(
    @JsonProperty(required=true) @NotNull @Min(0) @JsonDeserialize(using=StrictVersionDeserializer.class) Long expectedVersion,
    @JsonProperty(required=true) @NotBlank @Size(max=72) @Pattern(regexp="^(?=.*[A-Za-z])(?=.*[0-9]).{8,}$") String newPassword
) {@JsonAnySetter public void rejectUnknown(String name,Object value){throw new IllegalArgumentException("Unknown password reset field");}}
