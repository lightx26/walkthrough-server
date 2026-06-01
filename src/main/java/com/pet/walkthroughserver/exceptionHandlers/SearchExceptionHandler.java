package com.pet.walkthroughserver.exceptionHandlers;

import static com.pet.walkthroughserver.utils.ExceptionResponse.respond;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.pet.walkthroughserver.interceptors.ErrorResponse;
import com.pet.walkthroughserver.modules._shared.exceptions.AppException;
import com.pet.walkthroughserver.modules.search.exceptions.IndexingException;
import com.pet.walkthroughserver.modules.search.exceptions.SearchException;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestControllerAdvice
public class SearchExceptionHandler {

    @ExceptionHandler({SearchException.class, IndexingException.class})
    public ResponseEntity<ErrorResponse> handle(AppException ex) {
        log.warn("{}: {}", ex.getClass().getSimpleName(), ex.getMessage());
        return respond(ex);
    }
}
