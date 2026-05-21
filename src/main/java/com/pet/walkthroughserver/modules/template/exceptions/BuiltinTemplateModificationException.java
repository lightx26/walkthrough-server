package com.pet.walkthroughserver.modules.template.exceptions;

import com.pet.walkthroughserver.modules._shared.exceptions.AppException;
import org.springframework.http.HttpStatus;

public class BuiltinTemplateModificationException extends AppException {

    public BuiltinTemplateModificationException(String message) {
        super(HttpStatus.FORBIDDEN, "BUILTIN_TEMPLATE_MODIFICATION", message);
    }
}
