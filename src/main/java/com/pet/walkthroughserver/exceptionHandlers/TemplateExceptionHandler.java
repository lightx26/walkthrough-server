package com.pet.walkthroughserver.exceptionHandlers;

import com.pet.walkthroughserver.interceptors.ErrorResponse;
import com.pet.walkthroughserver.modules.template.exceptions.BuiltinTemplateModificationException;
import com.pet.walkthroughserver.modules.template.exceptions.TemplateAccessDeniedException;
import com.pet.walkthroughserver.modules.template.exceptions.TemplateNotFoundException;
import com.pet.walkthroughserver.modules.template.presentation.TemplateController;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import static com.pet.walkthroughserver.utils.ExceptionResponse.respond;

@Slf4j
@Order(1)
@RestControllerAdvice(basePackageClasses = TemplateController.class)
public class TemplateExceptionHandler {

    @ExceptionHandler(TemplateNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(TemplateNotFoundException ex) {
        log.warn("TemplateNotFoundException: {}", ex.getMessage());
        return respond(ex);
    }

    @ExceptionHandler(TemplateAccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(TemplateAccessDeniedException ex) {
        log.warn("TemplateAccessDeniedException: {}", ex.getMessage());
        return respond(ex);
    }

    @ExceptionHandler(BuiltinTemplateModificationException.class)
    public ResponseEntity<ErrorResponse> handleBuiltinModification(BuiltinTemplateModificationException ex) {
        log.warn("BuiltinTemplateModificationException: {}", ex.getMessage());
        return respond(ex);
    }
}
