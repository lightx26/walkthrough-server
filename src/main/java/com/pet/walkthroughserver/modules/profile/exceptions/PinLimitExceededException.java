package com.pet.walkthroughserver.modules.profile.exceptions;

import com.pet.walkthroughserver.modules._shared.exceptions.AppException;
import org.springframework.http.HttpStatus;

public class PinLimitExceededException extends AppException {

    public PinLimitExceededException() {
        super(HttpStatus.BAD_REQUEST, "PIN_LIMIT_EXCEEDED", "Maximum of 6 pinned walkthroughs allowed");
    }
}
