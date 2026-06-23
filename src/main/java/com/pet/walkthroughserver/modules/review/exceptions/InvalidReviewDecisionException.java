package com.pet.walkthroughserver.modules.review.exceptions;

import org.springframework.http.HttpStatus;

import com.pet.walkthroughserver.modules._shared.exceptions.AppException;

public class InvalidReviewDecisionException extends AppException {

    public InvalidReviewDecisionException(String message) {
        super(HttpStatus.BAD_REQUEST, "INVALID_DECISION", message);
    }
}
