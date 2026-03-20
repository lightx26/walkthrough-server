package com.pet.walkthroughserver.modules.auth.exceptions;

import com.pet.walkthroughserver.modules._shared.exceptions.AppException;
import org.springframework.http.HttpStatus;

public class NotAuthenticatedException extends AppException {

    public NotAuthenticatedException(String message) {
        super(HttpStatus.UNAUTHORIZED, "NOT_AUTHENTICATED", message);
    }
}
