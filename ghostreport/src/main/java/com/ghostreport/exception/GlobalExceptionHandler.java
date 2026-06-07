package com.ghostreport.exception;

import com.ghostreport.security.CorrelationId;
import com.ghostreport.service.AuditLogService;
import com.ghostreport.service.SecurityMonitoringService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private final AuditLogService auditLogService;
    private final SecurityMonitoringService securityMonitoringService;

    public GlobalExceptionHandler(
            AuditLogService auditLogService,
            SecurityMonitoringService securityMonitoringService
    ) {
        this.auditLogService = auditLogService;
        this.securityMonitoringService = securityMonitoringService;
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<Map<String, Object>> handleAuthenticationException(AuthenticationException ex) {
        Map<String, Object> body = errorBody(HttpStatus.UNAUTHORIZED.value(), "Invalid credentials");
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(body);
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, Object>> handleResponseStatusException(ResponseStatusException ex) {
        Map<String, Object> body = errorBody(ex.getStatusCode().value(), ex.getReason());
        return ResponseEntity.status(ex.getStatusCode()).body(body);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationException(MethodArgumentNotValidException ex) {
        Map<String, Object> body = new HashMap<>();
        Map<String, String> fieldErrors = new HashMap<>();

        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            fieldErrors.put(error.getField(), error.getDefaultMessage());
        }

        body.put("timestamp", Instant.now().toString());
        body.put("correlationId", CorrelationId.current());
        body.put("status", HttpStatus.BAD_REQUEST.value());
        body.put("error", "Validation failed");
        body.put("fields", fieldErrors);

        return ResponseEntity.badRequest().body(body);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<Map<String, Object>> handleMissingRequestParameter(MissingServletRequestParameterException ex) {
        Map<String, Object> body = errorBody(HttpStatus.BAD_REQUEST.value(), "Missing required request parameter");
        return ResponseEntity.badRequest().body(body);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> handleUnreadableMessage(HttpMessageNotReadableException ex) {
        Map<String, Object> body = errorBody(HttpStatus.BAD_REQUEST.value(), "Malformed request");
        return ResponseEntity.badRequest().body(body);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, Object>> handleAccessDeniedException(AccessDeniedException ex) {
        securityMonitoringService.recordForbiddenAccess("method-security");
        Map<String, Object> body = errorBody(HttpStatus.FORBIDDEN.value(), "Access denied");
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(body);
    }

    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public ResponseEntity<Map<String, Object>> handleOptimisticLockingFailure(
            ObjectOptimisticLockingFailureException ex
    ) {
        Map<String, Object> body = errorBody(
                HttpStatus.CONFLICT.value(),
                "Resource was modified by another transaction"
        );
        return ResponseEntity.status(HttpStatus.CONFLICT).body(body);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGenericException(Exception ex) {
        auditLogService.log(
                "UNEXPECTED_ERROR",
                "APPLICATION",
                null,
                "Unexpected error type=" + ex.getClass().getSimpleName()
        );
        securityMonitoringService.recordUnexpectedError(ex.getClass().getSimpleName());
        Map<String, Object> body = errorBody(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Unexpected internal error");
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }

    private Map<String, Object> errorBody(int status, String error) {
        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", Instant.now().toString());
        body.put("correlationId", CorrelationId.current());
        body.put("status", status);
        body.put("error", error);
        return body;
    }
}
