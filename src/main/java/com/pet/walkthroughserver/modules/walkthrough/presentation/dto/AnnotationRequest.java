package com.pet.walkthroughserver.modules.walkthrough.presentation.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AnnotationRequest {

    @NotNull
    @Min(1)
    private Integer startLine;

    @NotNull
    @Min(1)
    private Integer endLine;

    @NotBlank
    private String lineSide;

    @NotBlank
    private String content;
}
