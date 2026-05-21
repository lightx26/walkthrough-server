package com.pet.walkthroughserver.modules.template.exceptions;

import com.pet.walkthroughserver.modules._shared.exceptions.AppException;
import org.springframework.http.HttpStatus;

public class TemplateAccessDeniedException extends AppException {

    public TemplateAccessDeniedException(String message) {
        super(HttpStatus.FORBIDDEN, "TEMPLATE_ACCESS_DENIED", message);
    }
}
