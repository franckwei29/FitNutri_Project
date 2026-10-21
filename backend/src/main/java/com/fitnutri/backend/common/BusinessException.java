package com.fitnutri.backend.common;

import java.util.List;
import org.springframework.http.HttpStatus;

/** Only fixed, public-safe messages belong in this exception. */
public class BusinessException extends RuntimeException {
    private final HttpStatus status;
    private final String code;
    private final List<FieldViolation> fields;
    public BusinessException(HttpStatus status, String code, String message) {
        this(status, code, message, List.of());
    }
    public BusinessException(HttpStatus status, String code, String message, List<FieldViolation> fields) {
        super(message);
        this.status=status; this.code=code; this.fields=List.copyOf(fields);
    }
    public HttpStatus status() { return status; }
    public String code() { return code; }
    public List<FieldViolation> fields() { return fields; }
    public static BusinessException invalid(String field, String message) {
        return new BusinessException(HttpStatus.BAD_REQUEST,"VALIDATION_ERROR","Check the highlighted fields.",List.of(new FieldViolation(field,null,message)));
    }
}
