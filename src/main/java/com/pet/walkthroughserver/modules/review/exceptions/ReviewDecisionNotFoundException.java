package com.pet.walkthroughserver.modules.review.exceptions;

import org.springframework.http.HttpStatus;

import com.pet.walkthroughserver.modules._shared.exceptions.AppException;

public class ReviewDecisionNotFoundException extends AppException {

    public ReviewDecisionNotFoundException(String message) {
        super(HttpStatus.NOT_FOUND, "REVIEW_DECISION_NOT_FOUND", message);
    }
}
