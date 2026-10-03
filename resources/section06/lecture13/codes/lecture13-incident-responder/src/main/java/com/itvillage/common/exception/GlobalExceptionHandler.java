package com.itvillage.common.exception;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.OffsetDateTime;

@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(IncidentException.class)
    public ResponseEntity<ErrorResponse> handleIncidentException(
            IncidentException ex,
            HttpServletRequest request
    ) {
        return org.springframework.http.ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(
                        OffsetDateTime.now(),
                        "INCIDENT_ERROR",
                        ex.getMessage(),
                        request.getRequestURI()
                ));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(
            MethodArgumentNotValidException ex,
            HttpServletRequest request
    ) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .orElse("Invalid request");

        return org.springframework.http.ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(
                        OffsetDateTime.now(),
                        "VALIDATION_ERROR",
                        message,
                        request.getRequestURI()
                ));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolation(
            ConstraintViolationException ex,
            HttpServletRequest request
    ) {
        return org.springframework.http.ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(
                        OffsetDateTime.now(),
                        "CONSTRAINT_VIOLATION",
                        ex.getMessage(),
                        request.getRequestURI()
                ));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception ex,
                                                          HttpServletRequest request) {
        // 테스트용 로깅
        log.error("incident-request-failed. exceptionType={}, message={}",
                ex.getClass().getSimpleName(), ex.getMessage(), ex);

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse(
                        OffsetDateTime.now(),
                        "INTERNAL_ERROR",
                        ex.getMessage(),
                        request.getRequestURI()
                ));
    }
}