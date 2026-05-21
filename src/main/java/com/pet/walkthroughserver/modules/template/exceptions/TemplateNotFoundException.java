package com.pet.walkthroughserver.modules.template.exceptions;

import com.pet.walkthroughserver.modules._shared.exceptions.AppException;
import org.springframework.http.HttpStatus;

public class TemplateNotFoundException extends AppException {

    public TemplateNotFoundException(String message) {
        super(HttpStatus.NOT_FOUND, "TEMPLATE_NOT_FOUND", message);
    }
}
