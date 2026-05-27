package com.pet.walkthroughserver.modules.walkthrough.presentation.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;

import java.util.UUID;

@Getter
public class RecordChapterViewRequest {

    @NotNull(message = "Chapter ID is required")
    private UUID chapterId;

    private Integer timeSpentSec;

    private Boolean markedAsRead;
}
