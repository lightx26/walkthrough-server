package com.pet.walkthroughserver.exceptionHandlers;

import com.pet.walkthroughserver.interceptors.ErrorResponse;
import com.pet.walkthroughserver.modules.walkthrough.exceptions.WalkthroughAccessDeniedException;
import com.pet.walkthroughserver.modules.walkthrough.exceptions.WalkthroughNotFoundException;
import com.pet.walkthroughserver.modules.walkthrough.presentation.WalkthroughController;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import static com.pet.walkthroughserver.utils.ExceptionResponse.respond;

@Slf4j
@Order(1)
@RestControllerAdvice(basePackageClasses = WalkthroughController.class)
public class WalkthroughExceptionHandler {

    @ExceptionHandler(WalkthroughNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(WalkthroughNotFoundException ex) {
        log.warn("WalkthroughNotFoundException: {}", ex.getMessage());
        return respond(ex);
    }

    @ExceptionHandler(WalkthroughAccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(WalkthroughAccessDeniedException ex) {
        log.warn("WalkthroughAccessDeniedException: {}", ex.getMessage());
        return respond(ex);
    }
}
