package com.pet.walkthroughserver.exceptionHandlers;

import com.pet.walkthroughserver.interceptors.ErrorResponse;
import com.pet.walkthroughserver.modules.comment.exceptions.CommentNotFoundException;
import com.pet.walkthroughserver.modules.comment.presentation.CommentController;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import static com.pet.walkthroughserver.utils.ExceptionResponse.respond;

@Slf4j
@Order(1)
@RestControllerAdvice(basePackageClasses = CommentController.class)
public class CommentExceptionHandler {

    @ExceptionHandler(CommentNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(CommentNotFoundException ex) {
        log.warn("CommentNotFoundException: {}", ex.getMessage());
        return respond(ex);
    }
}
