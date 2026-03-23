package com.pet.walkthroughserver.modules.walkthrough.exceptions;

import com.pet.walkthroughserver.modules._shared.exceptions.AppException;
import org.springframework.http.HttpStatus;

public class WalkthroughNotFoundException extends AppException {

    public WalkthroughNotFoundException(String message) {
        super(HttpStatus.NOT_FOUND, "WALKTHROUGH_NOT_FOUND", message);
    }
}
