package com.pet.walkthroughserver.exceptionHandlers;

import com.pet.walkthroughserver.interceptors.ErrorResponse;
import com.pet.walkthroughserver.modules._shared.infra.ai.exceptions.LlmApiException;
import com.pet.walkthroughserver.modules.riskzone.exceptions.RiskScanInProgressException;
import com.pet.walkthroughserver.modules.riskzone.exceptions.RiskScanNotFoundException;
import com.pet.walkthroughserver.modules.riskzone.presentation.RiskController;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@Order(2)
@RestControllerAdvice(basePackageClasses = RiskController.class)
public class RiskExceptionHandler {

    @ExceptionHandler(RiskScanNotFoundException.class)
    public ResponseEntity<ErrorResponse> handle(RiskScanNotFoundException ex) {
        log.warn("RiskScanNotFound: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ErrorResponse.of(ex.getErrorCode(), ex.getMessage()));
    }

    @ExceptionHandler(RiskScanInProgressException.class)
    public ResponseEntity<ErrorResponse> handle(RiskScanInProgressException ex) {
        log.warn("RiskScanInProgress: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ErrorResponse.of(ex.getErrorCode(), ex.getMessage()));
    }

    @ExceptionHandler(LlmApiException.class)
    public ResponseEntity<ErrorResponse> handle(LlmApiException ex) {
        log.error("LlmApiError: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                .body(ErrorResponse.of(ex.getErrorCode(), ex.getMessage()));
    }
}
