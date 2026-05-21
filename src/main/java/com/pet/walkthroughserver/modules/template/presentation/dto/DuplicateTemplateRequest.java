package com.pet.walkthroughserver.modules.template.presentation.dto;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DuplicateTemplateRequest {

    @Size(max = 255)
    private String name;
}
