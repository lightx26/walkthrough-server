package com.pet.walkthroughserver.modules.walkthrough.presentation.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

import java.util.UUID;

@Getter
public class CreateCommentRequest {

    @NotBlank(message = "Content is required")
    private String content;

    private UUID chapterId;

    private UUID walkthroughFileId;

    private Integer diffPosition;

    private UUID parentId;
}
