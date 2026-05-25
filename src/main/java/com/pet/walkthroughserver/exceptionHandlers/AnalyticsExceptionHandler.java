package com.pet.walkthroughserver.exceptionHandlers;

import com.pet.walkthroughserver.interceptors.ErrorResponse;
import com.pet.walkthroughserver.modules.analytics.presentation.AnalyticsController;
import com.pet.walkthroughserver.modules.walkthrough.exceptions.WalkthroughAccessDeniedException;
import com.pet.walkthroughserver.modules.walkthrough.exceptions.WalkthroughNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import static com.pet.walkthroughserver.utils.ExceptionResponse.respond;

@Slf4j
@Order(1)
@RestControllerAdvice(basePackageClasses = AnalyticsController.class)
public class AnalyticsExceptionHandler {

    @ExceptionHandler(WalkthroughNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(WalkthroughNotFoundException ex) {
        log.warn("WalkthroughNotFoundException in analytics context: {}", ex.getMessage());
        return respond(ex);
    }

    @ExceptionHandler(WalkthroughAccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(WalkthroughAccessDeniedException ex) {
        log.warn("WalkthroughAccessDeniedException in analytics context: {}", ex.getMessage());
        return respond(ex);
    }
}
