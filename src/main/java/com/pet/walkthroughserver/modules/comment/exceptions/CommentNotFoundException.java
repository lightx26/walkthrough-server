package com.pet.walkthroughserver.modules.comment.exceptions;

import com.pet.walkthroughserver.modules._shared.exceptions.AppException;
import org.springframework.http.HttpStatus;

public class CommentNotFoundException extends AppException {

    public CommentNotFoundException(String message) {
        super(HttpStatus.NOT_FOUND, "COMMENT_NOT_FOUND", message);
    }
}
