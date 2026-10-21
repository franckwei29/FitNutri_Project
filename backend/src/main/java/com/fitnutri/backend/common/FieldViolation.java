package com.fitnutri.backend.common;

/** Public field identity and guidance; never includes the rejected value. */
public record FieldViolation(String field, String clientEntryId, String message) {}
