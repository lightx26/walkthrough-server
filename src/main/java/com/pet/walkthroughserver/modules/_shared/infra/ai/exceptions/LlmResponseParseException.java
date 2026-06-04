package com.pet.walkthroughserver.modules._shared.infra.ai.exceptions;

import com.pet.walkthroughserver.modules._shared.exceptions.AppException;
import org.springframework.http.HttpStatus;

public class LlmResponseParseException extends AppException {

    public LlmResponseParseException(String message) {
        super(HttpStatus.INTERNAL_SERVER_ERROR, "LLM_PARSE_ERROR", message);
    }

    public LlmResponseParseException(String message, Throwable cause) {
        super(HttpStatus.INTERNAL_SERVER_ERROR, "LLM_PARSE_ERROR", message, cause);
    }
}
