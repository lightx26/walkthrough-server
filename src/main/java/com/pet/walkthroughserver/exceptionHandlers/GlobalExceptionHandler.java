package com.pet.walkthroughserver.exceptionHandlers;

import java.util.HashMap;
import java.util.Map;

import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import com.pet.walkthroughserver.interceptors.ErrorResponse;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Order(2)
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationExceptions(
            MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });
        log.warn("Validation failed: {}", errors);
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.of(
                        "VALIDATION_FAILED",
                        "Validation failed",
                        errors));
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleMissingParams(
            MissingServletRequestParameterException ex) {
        String message = String.format("Missing required parameter: %s", ex.getParameterName());
        log.warn("Missing parameter: {}", ex.getParameterName());
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.of(
                        "MISSING_PARAMETER",
                        message));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(
            MethodArgumentTypeMismatchException ex) {
        String message = String.format("Invalid value for parameter '%s': %s",
                ex.getName(), ex.getValue());
        log.warn("Type mismatch for parameter {}: {}", ex.getName(), ex.getValue());
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.of(
                        "TYPE_MISMATCH",
                        message));
    }

    @ExceptionHandler(AuthorizationDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAuthorizationDenied(AuthorizationDeniedException ex) {
        log.debug("Authorization denied: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(ErrorResponse.of(
                        "NOT_AUTHENTICATED",
                        "Authentication required"));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception ex) {
        log.error("Unexpected error", ex);
        return ResponseEntity
                .internalServerError()
                .body(ErrorResponse.of(
                        "INTERNAL_SERVER_ERROR",
                        "Internal server error"));
    }
}
