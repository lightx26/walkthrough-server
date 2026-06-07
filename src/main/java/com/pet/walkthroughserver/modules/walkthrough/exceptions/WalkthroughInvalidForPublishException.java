package com.pet.walkthroughserver.modules.walkthrough.exceptions;

import com.pet.walkthroughserver.modules._shared.exceptions.AppException;
import org.springframework.http.HttpStatus;

public class WalkthroughInvalidForPublishException extends AppException {

    public WalkthroughInvalidForPublishException(String message) {
        super(HttpStatus.BAD_REQUEST, "WALKTHROUGH_INVALID_FOR_PUBLISH", message);
    }
}
