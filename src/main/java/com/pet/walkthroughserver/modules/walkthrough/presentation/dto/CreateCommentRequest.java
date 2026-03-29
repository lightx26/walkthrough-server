package com.pet.walkthroughserver.modules.walkthrough.presentation.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

@Getter
public class CreateCommentRequest {

    @NotBlank(message = "Content is required")
    private String content;
}
