package com.pet.walkthroughserver.modules.template.presentation.dto;

import com.pet.walkthroughserver.modules.template.repository.TemplatePrType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class UpdateTemplateRequest {

    @Size(max = 255)
    private String name;

    private String description;

    private TemplatePrType prType;

    @Valid
    private List<TemplateChapterRequest> chapters;
}
