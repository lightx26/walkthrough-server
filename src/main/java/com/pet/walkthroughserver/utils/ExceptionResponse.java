package com.pet.walkthroughserver.utils;

import com.pet.walkthroughserver.interceptors.ErrorResponse;
import com.pet.walkthroughserver.modules._shared.exceptions.AppException;
import org.springframework.http.ResponseEntity;

public class ExceptionResponse {
    public static ResponseEntity<ErrorResponse> respond(AppException ex) {
        return ResponseEntity
                .status(ex.getHttpStatus())
                .body(ErrorResponse.of(ex.getErrorCode(), ex.getMessage()));
    }
}
