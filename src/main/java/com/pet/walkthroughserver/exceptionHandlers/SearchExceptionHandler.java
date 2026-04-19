package com.pet.walkthroughserver.exceptionHandlers;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.pet.walkthroughserver.interceptors.ErrorResponse;
import com.pet.walkthroughserver.modules.search.exceptions.IndexingException;
import com.pet.walkthroughserver.modules.search.exceptions.SearchException;

@RestControllerAdvice
public class SearchExceptionHandler {

    @ExceptionHandler(SearchException.class)
    public ResponseEntity<ErrorResponse> handleSearchException(SearchException e) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(ErrorResponse.of("SEARCH_ERROR", e.getMessage()));
    }

    @ExceptionHandler(IndexingException.class)
    public ResponseEntity<ErrorResponse> handleIndexingException(IndexingException e) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(ErrorResponse.of("INDEXING_ERROR", e.getMessage()));
    }
}
