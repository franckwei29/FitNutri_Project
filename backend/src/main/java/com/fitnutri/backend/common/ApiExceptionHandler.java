package com.fitnutri.backend.common;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice
public class ApiExceptionHandler {
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiError> business(BusinessException error,HttpServletRequest request) {
        var result=build(error.status().value(),error.code(),error.getMessage(),error.fields(),request);
        if(error instanceof RateLimitedException limited)return ResponseEntity.status(result.getStatusCode()).headers(result.getHeaders()).header("Retry-After",Integer.toString(limited.retryAfterSeconds())).body(result.getBody());
        return result;
    }
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> validation(MethodArgumentNotValidException error,HttpServletRequest request) {
        List<FieldViolation> fields=error.getBindingResult().getFieldErrors().stream()
            .map(e->new FieldViolation(e.getField(),null,"Enter a valid value for this field.")).toList();
        return build(400,"VALIDATION_ERROR","Check the highlighted fields.",fields,request);
    }
    @ExceptionHandler({HttpMessageNotReadableException.class,MethodArgumentTypeMismatchException.class,MissingServletRequestParameterException.class,MissingServletRequestPartException.class})
    public ResponseEntity<ApiError> malformed(Exception error,HttpServletRequest request) {
        if(error instanceof MissingServletRequestParameterException missing)
            return build(400,"VALIDATION_ERROR","A required parameter is missing.",List.of(new FieldViolation(missing.getParameterName(),null,"Provide this required parameter.")),request);
        if(error instanceof MissingServletRequestPartException missing)
            return build(400,"VALIDATION_ERROR","A required upload part is missing.",List.of(new FieldViolation(missing.getRequestPartName(),null,"Provide this required upload part.")),request);
        if(error instanceof MethodArgumentTypeMismatchException mismatch)
            return build(400,"VALIDATION_ERROR","A parameter has an invalid format.",List.of(new FieldViolation(mismatch.getName(),null,"Enter a value in the required format.")),request);
        return build(400,"VALIDATION_ERROR","The request is malformed or contains an invalid value.",List.of(),request);
    }
    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public ResponseEntity<ApiError> version(Exception ignored,HttpServletRequest request) {
        return build(409,"VERSION_CONFLICT","Reload and review the latest version.",List.of(),request);
    }
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiError> integrity(Exception ignored,HttpServletRequest request) {
        return build(409,"CONSTRAINT_CONFLICT","The request conflicts with an existing record.",List.of(),request);
    }
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ApiError> legacy(ResponseStatusException error,HttpServletRequest request) {
        int status=error.getStatusCode().value();
        String code=switch(status) {
            case 400 -> "VALIDATION_ERROR"; case 401 -> "UNAUTHENTICATED"; case 403 -> "FORBIDDEN";
            case 404 -> "NOT_FOUND"; case 409 -> "CONSTRAINT_CONFLICT"; case 422 -> "UNSUPPORTED_REFERENCE";
            case 429 -> "RATE_LIMITED"; case 502 -> "PROVIDER_BAD_RESPONSE"; case 503 -> "FEATURE_NOT_CONFIGURED";
            case 504 -> "PROVIDER_TIMEOUT"; case 500 -> "INTERNAL_ERROR"; default -> "REQUEST_REJECTED";
        };
        return build(status,code,"The request could not be completed.",List.of(),request);
    }
    @ExceptionHandler(org.springframework.web.multipart.MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiError> oversizedUpload(Exception ignored,HttpServletRequest request) {
        return build(413,"PAYLOAD_TOO_LARGE","The uploaded request exceeds the supported size.",List.of(),request);
    }
    @ExceptionHandler({org.springframework.web.servlet.resource.NoResourceFoundException.class,
        org.springframework.web.servlet.NoHandlerFoundException.class,
        org.springframework.web.HttpRequestMethodNotSupportedException.class,
        org.springframework.web.HttpMediaTypeNotSupportedException.class,
        org.springframework.web.HttpMediaTypeNotAcceptableException.class})
    public ResponseEntity<ApiError> protocol(Exception error,HttpServletRequest request) {
        var framework=(org.springframework.web.ErrorResponse)error;
        int status=framework.getStatusCode().value();
        var safe=build(status,status==404?"NOT_FOUND":"REQUEST_REJECTED","The requested resource or request format is not supported.",List.of(),request);
        return ResponseEntity.status(status).headers(framework.getHeaders()).headers(safe.getHeaders()).body(safe.getBody());
    }
    @ExceptionHandler({org.springframework.transaction.CannotCreateTransactionException.class,org.springframework.dao.DataAccessResourceFailureException.class,org.springframework.dao.TransientDataAccessResourceException.class})
    public ResponseEntity<ApiError> databaseUnavailable(Exception ignored,HttpServletRequest request){return build(503,"DATABASE_UNAVAILABLE","The database is temporarily unavailable. Check the outcome of any save before retrying.",List.of(),request);}
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> unexpected(Exception ignored,HttpServletRequest request) {
        return build(500,"INTERNAL_ERROR","The request could not be completed.",List.of(),request);
    }
    private ResponseEntity<ApiError> build(int status,String code,String message,List<FieldViolation> fields,HttpServletRequest request) {
        return ResponseEntity.status(status).header("Cache-Control","no-store")
            .body(ApiError.create(status,code,message,request,fields,status==429 || status==502 || status==503 || status==504));
    }
}
