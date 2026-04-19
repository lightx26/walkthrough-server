package com.pet.walkthroughserver.modules.profile.exceptions;

import com.pet.walkthroughserver.modules._shared.exceptions.AppException;
import org.springframework.http.HttpStatus;

public class AlreadyPinnedException extends AppException {

    public AlreadyPinnedException() {
        super(HttpStatus.CONFLICT, "ALREADY_PINNED", "Walkthrough is already pinned");
    }
}
