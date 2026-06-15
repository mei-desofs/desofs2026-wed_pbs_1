package com.ghostreport.exception;

import com.ghostreport.security.CorrelationId;
import com.ghostreport.service.AuditLogService;
import com.ghostreport.service.SecurityMonitoringService;
import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.mock.http.MockHttpInputMessage;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.server.ResponseStatusException;

import java.lang.reflect.Method;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class GlobalExceptionHandlerTest {

    private static final String CORRELATION_ID = "test-correlation-id";

    private final AuditLogService auditLogService =
            mock(AuditLogService.class);
    private final SecurityMonitoringService securityMonitoringService =
            mock(SecurityMonitoringService.class);
    private final GlobalExceptionHandler handler =
            new GlobalExceptionHandler(auditLogService, securityMonitoringService);

    @BeforeEach
    void setCorrelationId() {
        CorrelationId.set(CORRELATION_ID);
    }

    @AfterEach
    void clearCorrelationId() {
        CorrelationId.clear();
    }

    @Test
    void authenticationExceptionReturnsSanitizedUnauthorizedBody() {
        ResponseEntity<Map<String, Object>> response =
                handler.handleAuthenticationException(
                        new BadCredentialsException("raw details")
                );

        assertErrorResponse(
                response,
                HttpStatus.UNAUTHORIZED,
                "Invalid credentials"
        );
    }

    @ParameterizedTest
    @CsvSource({
            "400, Invalid request",
            "401, Unauthorized",
            "403, Access denied",
            "404, Resource not found",
            "409, Request conflict",
            "415, Unsupported media type",
            "429, Too many requests",
            "418, Request failed",
            "500, Unexpected internal error"
    })
    void responseStatusExceptionUsesSafeErrorMessages(
            int statusCode,
            String expectedError
    ) {
        HttpStatus status = HttpStatus.valueOf(statusCode);

        ResponseEntity<Map<String, Object>> response =
                handler.handleResponseStatusException(
                        new ResponseStatusException(status, "sensitive details")
                );

        assertErrorResponse(response, status, expectedError);
    }

    @Test
    void validationExceptionReturnsFieldMapWithoutRawRejectedValues() throws Exception {
        BeanPropertyBindingResult bindingResult =
                new BeanPropertyBindingResult(new Object(), "request");
        bindingResult.addError(
                new FieldError(
                        "request",
                        "description",
                        "<script>alert(1)</script>"
                )
        );

        MethodArgumentNotValidException exception =
                new MethodArgumentNotValidException(
                        dummyMethodParameter(),
                        bindingResult
                );

        ResponseEntity<Map<String, Object>> response =
                handler.handleValidationException(exception);

        assertErrorResponse(response, HttpStatus.BAD_REQUEST, "Invalid request");
        Object fieldsObject = body(response).get("fields");
        assertInstanceOf(Map.class, fieldsObject);
        Map<?, ?> fields = (Map<?, ?>) fieldsObject;
        assertEquals("Invalid value", fields.get("description"));
    }

    @Test
    void missingRequestParameterReturnsInvalidRequest() {
        ResponseEntity<Map<String, Object>> response =
                handler.handleMissingRequestParameter(
                        new MissingServletRequestParameterException(
                                "trackingCode",
                                "String"
                        )
                );

        assertErrorResponse(response, HttpStatus.BAD_REQUEST, "Invalid request");
    }

    @Test
    void missingRequestPartReturnsInvalidRequest() {
        ResponseEntity<Map<String, Object>> response =
                handler.handleMissingRequestPart(
                        new MissingServletRequestPartException("file")
                );

        assertErrorResponse(response, HttpStatus.BAD_REQUEST, "Invalid request");
    }

    @Test
    void unreadableMessageReturnsMalformedRequest() {
        ResponseEntity<Map<String, Object>> response =
                handler.handleUnreadableMessage(
                        new HttpMessageNotReadableException(
                                "raw parser details",
                                new MockHttpInputMessage(new byte[0])
                        )
                );

        assertErrorResponse(response, HttpStatus.BAD_REQUEST, "Malformed request");
    }

    @Test
    void constraintViolationReturnsInvalidRequest() {
        ResponseEntity<Map<String, Object>> response =
                handler.handleConstraintViolation(
                        new ConstraintViolationException("raw violation", null)
                );

        assertErrorResponse(response, HttpStatus.BAD_REQUEST, "Invalid request");
    }

    @Test
    void unsupportedMediaTypeReturnsJson415() {
        ResponseEntity<Map<String, Object>> response =
                handler.handleUnsupportedMediaType(
                        new HttpMediaTypeNotSupportedException(
                                MediaType.TEXT_PLAIN_VALUE
                        )
                );

        assertErrorResponse(
                response,
                HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                "Unsupported media type"
        );
    }

    @Test
    void accessDeniedRecordsSecurityEventAndReturnsForbidden() {
        ResponseEntity<Map<String, Object>> response =
                handler.handleAccessDeniedException(
                        new AccessDeniedException("raw method details")
                );

        assertErrorResponse(response, HttpStatus.FORBIDDEN, "Access denied");
        verify(securityMonitoringService).recordForbiddenAccess("method-security");
    }

    @Test
    void optimisticLockingFailureReturnsConflictWithoutRawExceptionDetails() {
        ResponseEntity<Map<String, Object>> response =
                handler.handleOptimisticLockingFailure(
                        new ObjectOptimisticLockingFailureException(
                                Object.class,
                                1L
                        )
                );

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertEquals(
                "Resource was modified by another transaction",
                body(response).get("error")
        );
        assertEquals(CORRELATION_ID, body(response).get("correlationId"));
    }

    @Test
    void genericExceptionAuditsAndReturnsSanitizedInternalError() {
        ResponseEntity<Map<String, Object>> response =
                handler.handleGenericException(
                        new IllegalStateException("database password leaked")
                );

        assertErrorResponse(
                response,
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Unexpected internal error"
        );
        verify(auditLogService).log(
                "UNEXPECTED_ERROR",
                "APPLICATION",
                null,
                "Unexpected error type=IllegalStateException"
        );
        verify(securityMonitoringService)
                .recordUnexpectedError("IllegalStateException");
    }

    private void assertErrorResponse(
            ResponseEntity<Map<String, Object>> response,
            HttpStatus expectedStatus,
            String expectedError
    ) {
        assertEquals(expectedStatus, response.getStatusCode());
        assertEquals(MediaType.APPLICATION_JSON, response.getHeaders().getContentType());
        assertEquals(expectedStatus.value(), body(response).get("status"));
        assertEquals(expectedError, body(response).get("error"));
        assertEquals(CORRELATION_ID, body(response).get("correlationId"));
        assertNotNull(body(response).get("timestamp"));
    }

    private Map<String, Object> body(
            ResponseEntity<Map<String, Object>> response
    ) {
        Map<String, Object> body = response.getBody();
        assertNotNull(body);
        return body;
    }

    private MethodParameter dummyMethodParameter() throws NoSuchMethodException {
        Method method = GlobalExceptionHandlerTest.class
                .getDeclaredMethod("dummyValidationTarget", String.class);
        return new MethodParameter(method, 0);
    }

    @SuppressWarnings("unused")
    private void dummyValidationTarget(String value) {
    }
}
