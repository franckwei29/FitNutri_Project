package com.fitnutri.backend.user;
import jakarta.validation.constraints.*;
public record RegisterRequest(
    @NotBlank @Email @Size(max=254) String email,
    @NotBlank @Size(max=72) @Pattern(regexp="^(?=.*[A-Za-z])(?=.*\\d).{8,}$",message="Use at least eight characters including a letter and a number.") String password,
    @NotBlank @Size(max=100) String displayName
) {
    public RegisterRequest {email=EmailCanonicalizer.canonical(email);displayName=displayName==null?null:displayName.trim();}
}
