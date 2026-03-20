package com.pet.walkthroughserver.modules._shared.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;

@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
public abstract class ApiResponse {

    private final boolean success;
    private final String message;

    protected ApiResponse(boolean success, String message) {
        this.success = success;
        this.message = message;
    }
}
