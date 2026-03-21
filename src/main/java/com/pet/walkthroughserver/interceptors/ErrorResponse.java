package com.pet.walkthroughserver.interceptors;

import lombok.Getter;

import java.util.Map;

@Getter
public class ErrorResponse extends ApiResponse {

    private final String errorCode;
    private final Map<String, String> errors;

    private ErrorResponse(String errorCode, String message, Map<String, String> errors) {
        super(false, message);
        this.errorCode = errorCode;
        this.errors = errors;
    }

    public static ErrorResponse of(String errorCode, String message) {
        return new ErrorResponse(errorCode, message, null);
    }

    public static ErrorResponse of(String errorCode, String message, Map<String, String> errors) {
        return new ErrorResponse(errorCode, message, errors);
    }
}
