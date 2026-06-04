package com.pet.walkthroughserver.modules._shared.infra.ai.exceptions;

import com.pet.walkthroughserver.modules._shared.exceptions.AppException;
import org.springframework.http.HttpStatus;

public class LlmApiException extends AppException {

    public LlmApiException(String message) {
        super(HttpStatus.BAD_GATEWAY, "LLM_API_ERROR", message);
    }

    public LlmApiException(String message, Throwable cause) {
        super(HttpStatus.BAD_GATEWAY, "LLM_API_ERROR", message, cause);
    }
}
