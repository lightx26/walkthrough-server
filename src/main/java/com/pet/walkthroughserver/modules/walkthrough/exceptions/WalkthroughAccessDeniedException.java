package com.pet.walkthroughserver.modules.walkthrough.exceptions;

import com.pet.walkthroughserver.modules._shared.exceptions.AppException;
import org.springframework.http.HttpStatus;

public class WalkthroughAccessDeniedException extends AppException {

    public WalkthroughAccessDeniedException(String message) {
        super(HttpStatus.FORBIDDEN, "WALKTHROUGH_ACCESS_DENIED", message);
    }
}
