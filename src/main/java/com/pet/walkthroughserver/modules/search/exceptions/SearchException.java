package com.pet.walkthroughserver.modules.search.exceptions;

import org.springframework.http.HttpStatus;

import com.pet.walkthroughserver.modules._shared.exceptions.AppException;

public class SearchException extends AppException {

    public SearchException(String message) {
        super(HttpStatus.SERVICE_UNAVAILABLE, "SEARCH_ERROR", message);
    }

    public SearchException(String message, Throwable cause) {
        super(HttpStatus.SERVICE_UNAVAILABLE, "SEARCH_ERROR", message, cause);
    }
}
