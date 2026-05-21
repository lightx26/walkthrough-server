package com.pet.walkthroughserver.modules.template.presentation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TemplateChapterRequest {

    @NotBlank
    @Size(max = 500)
    private String title;

    private String description;

    @NotNull
    private Integer sortOrder;
}
