package com.pet.walkthroughserver.modules.search.exceptions;

import org.springframework.http.HttpStatus;

import com.pet.walkthroughserver.modules._shared.exceptions.AppException;

public class IndexingException extends AppException {

    public IndexingException(String message) {
        super(HttpStatus.SERVICE_UNAVAILABLE, "INDEXING_ERROR", message);
    }

    public IndexingException(String message, Throwable cause) {
        super(HttpStatus.SERVICE_UNAVAILABLE, "INDEXING_ERROR", message, cause);
    }
}
