package com.boon.bank.controller.advice;

import com.boon.bank.dto.common.ErrorResponse;
import com.boon.bank.exception.ErrorCode;
import com.boon.bank.exception.business.BusinessException;
import com.boon.bank.exception.business.ExportTooLargeException;
import com.boon.bank.exception.business.ForbiddenException;
import com.boon.bank.exception.system.ExternalServiceException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<ErrorResponse> handleForbidden(ForbiddenException ex, HttpServletRequest req) {
        log.debug("Forbidden: {}", ex.getMessage());
        ErrorCode code = ErrorCode.FORBIDDEN;
        return ResponseEntity.status(code.getHttpStatus()).body(build(
                "https://bvb.local/errors/" + code.getCode(),
                code.getHttpStatus().getReasonPhrase(),
                code.getHttpStatus().value(),
                "Access denied",
                req.getRequestURI(),
                code.getCode(),
                null));
    }

    @ExceptionHandler(ExportTooLargeException.class)
    public ResponseEntity<Map<String, Object>> handleExportTooLarge(ExportTooLargeException ex,
                                                                    HttpServletRequest req) {
        return ResponseEntity.status(ex.getErrorCode().getHttpStatus()).body(Map.of(
                "code", ex.getErrorCode().getCode(),
                "error", "EXPORT_TOO_LARGE",
                "detail", ex.getMessage(),
                "rowCount", ex.getRowCount(),
                "maxRows", ex.getMaxRows(),
                "instance", req.getRequestURI(),
                "timestamp", Instant.now().toString()));
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusiness(BusinessException ex, HttpServletRequest req) {
        ErrorCode code = ex.getErrorCode();
        return ResponseEntity.status(code.getHttpStatus()).body(build(
                "https://bvb.local/errors/" + code.getCode(),
                code.getHttpStatus().getReasonPhrase(),
                code.getHttpStatus().value(),
                ex.getMessage(),
                req.getRequestURI(),
                code.getCode(),
                null));
    }

    @ExceptionHandler(ExternalServiceException.class)
    public ResponseEntity<ErrorResponse> handleExternal(ExternalServiceException ex, HttpServletRequest req) {
        ErrorCode code = ex.getErrorCode();
        return ResponseEntity.status(code.getHttpStatus()).body(build(
                "https://bvb.local/errors/" + code.getCode(),
                code.getHttpStatus().getReasonPhrase(),
                code.getHttpStatus().value(),
                ex.getMessage(),
                req.getRequestURI(),
                code.getCode(),
                null));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex,
                                                          HttpServletRequest req) {
        List<ErrorResponse.FieldError> fields = ex.getBindingResult().getFieldErrors().stream()
                .map(f -> new ErrorResponse.FieldError(f.getField(), f.getDefaultMessage()))
                .toList();
        ErrorCode code = ErrorCode.VALIDATION_FAILED;
        return ResponseEntity.status(code.getHttpStatus()).body(build(
                "https://bvb.local/errors/validation", "Validation failed",
                code.getHttpStatus().value(), ex.getMessage(), req.getRequestURI(),
                code.getCode(), fields));
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleAuth(AuthenticationException ex, HttpServletRequest req) {
        ErrorCode code = ErrorCode.UNAUTHORIZED;
        return ResponseEntity.status(code.getHttpStatus()).body(build(
                "https://bvb.local/errors/auth", code.getHttpStatus().getReasonPhrase(),
                code.getHttpStatus().value(), ex.getMessage(), req.getRequestURI(),
                code.getCode(), null));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException ex, HttpServletRequest req) {
        log.debug("AccessDenied: {}", ex.getMessage());
        ErrorCode code = ErrorCode.FORBIDDEN;
        return ResponseEntity.status(code.getHttpStatus()).body(build(
                "https://bvb.local/errors/forbidden", code.getHttpStatus().getReasonPhrase(),
                code.getHttpStatus().value(), "Access denied", req.getRequestURI(),
                code.getCode(), null));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException ex,
                                                            HttpServletRequest req) {
        log.debug("Type mismatch for param '{}': {}", ex.getName(), ex.getMessage());
        ErrorCode code = ErrorCode.VALIDATION_FAILED;
        return ResponseEntity.status(code.getHttpStatus()).body(build(
                "https://bvb.local/errors/validation", "Validation failed",
                code.getHttpStatus().value(), "Invalid request parameter", req.getRequestURI(),
                code.getCode(), null));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleOther(Exception ex, HttpServletRequest req) {
        log.error("Unhandled exception", ex);
        ErrorCode code = ErrorCode.INTERNAL_ERROR;
        return ResponseEntity.status(code.getHttpStatus()).body(build(
                "https://bvb.local/errors/internal", code.getHttpStatus().getReasonPhrase(),
                code.getHttpStatus().value(), "Internal server error", req.getRequestURI(),
                code.getCode(), null));
    }

    private ErrorResponse build(String type, String title, int status, String detail,
                                String instance, String code,
                                List<ErrorResponse.FieldError> errors) {
        return new ErrorResponse(type, title, status, detail, instance, code,
                MDC.get(TraceIdInterceptor.MDC_KEY), Instant.now(), errors);
    }
}
