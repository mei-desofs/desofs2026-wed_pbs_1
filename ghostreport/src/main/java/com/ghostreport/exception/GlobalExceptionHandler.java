package com.ghostreport.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.server.ResponseStatusException;

import jakarta.validation.ConstraintViolationException;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<Map<String, Object>> handleAuthenticationException(AuthenticationException ex) {
        Map<String, Object> body = errorBody(HttpStatus.UNAUTHORIZED.value(), "Invalid credentials");
        return json(HttpStatus.UNAUTHORIZED).body(body);
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, Object>> handleResponseStatusException(ResponseStatusException ex) {
        HttpStatusCode status = ex.getStatusCode();
        Map<String, Object> body = errorBody(status.value(), safeError(status));
        return json(status).body(body);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationException(MethodArgumentNotValidException ex) {
        Map<String, Object> body = new HashMap<>();
        Map<String, String> fieldErrors = new HashMap<>();

        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            fieldErrors.put(error.getField(), "Invalid value");
        }

        body.put("timestamp", OffsetDateTime.now());
        body.put("correlationId", UUID.randomUUID().toString());
        body.put("status", HttpStatus.BAD_REQUEST.value());
        body.put("error", "Invalid request");
        body.put("fields", fieldErrors);

        return json(HttpStatus.BAD_REQUEST).body(body);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<Map<String, Object>> handleMissingRequestParameter(MissingServletRequestParameterException ex) {
        Map<String, Object> body = errorBody(HttpStatus.BAD_REQUEST.value(), "Invalid request");
        return json(HttpStatus.BAD_REQUEST).body(body);
    }

    @ExceptionHandler(MissingServletRequestPartException.class)
    public ResponseEntity<Map<String, Object>> handleMissingRequestPart(MissingServletRequestPartException ex) {
        Map<String, Object> body = errorBody(HttpStatus.BAD_REQUEST.value(), "Invalid request");
        return json(HttpStatus.BAD_REQUEST).body(body);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> handleUnreadableMessage(HttpMessageNotReadableException ex) {
        Map<String, Object> body = errorBody(HttpStatus.BAD_REQUEST.value(), "Malformed request");
        return json(HttpStatus.BAD_REQUEST).body(body);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Map<String, Object>> handleConstraintViolation(ConstraintViolationException ex) {
        Map<String, Object> body = errorBody(HttpStatus.BAD_REQUEST.value(), "Invalid request");
        return json(HttpStatus.BAD_REQUEST).body(body);
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<Map<String, Object>> handleUnsupportedMediaType(HttpMediaTypeNotSupportedException ex) {
        Map<String, Object> body = errorBody(HttpStatus.UNSUPPORTED_MEDIA_TYPE.value(), "Unsupported media type");
        return json(HttpStatus.UNSUPPORTED_MEDIA_TYPE).body(body);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, Object>> handleAccessDeniedException(AccessDeniedException ex) {
        Map<String, Object> body = errorBody(HttpStatus.FORBIDDEN.value(), "Access denied");
        return json(HttpStatus.FORBIDDEN).body(body);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGenericException(Exception ex) {
        Map<String, Object> body = errorBody(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Unexpected internal error");
        return json(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }

    private Map<String, Object> errorBody(int status, String error) {
        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", OffsetDateTime.now());
        body.put("correlationId", UUID.randomUUID().toString());
        body.put("status", status);
        body.put("error", error);
        return body;
    }

    private ResponseEntity.BodyBuilder json(HttpStatusCode status) {
        return ResponseEntity.status(status).contentType(MediaType.APPLICATION_JSON);
    }

    private String safeError(HttpStatusCode status) {
        int value = status.value();
        return switch (value) {
            case 400 -> "Invalid request";
            case 401 -> "Unauthorized";
            case 403 -> "Access denied";
            case 404 -> "Resource not found";
            case 409 -> "Request conflict";
            case 415 -> "Unsupported media type";
            case 429 -> "Too many requests";
            default -> value >= 500 ? "Unexpected internal error" : "Request failed";
        };
    }
}
