package com.pet.walkthroughserver.modules.profile.exceptions;

import com.pet.walkthroughserver.modules._shared.exceptions.AppException;
import org.springframework.http.HttpStatus;

public class PinNotFoundException extends AppException {

    public PinNotFoundException() {
        super(HttpStatus.NOT_FOUND, "PIN_NOT_FOUND", "Pin not found");
    }
}
