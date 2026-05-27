package com.pet.walkthroughserver.modules.walkthrough.presentation.dto;

import java.util.UUID;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;

@Getter
public class RecordChapterViewRequest {

    @NotNull(message = "Chapter ID is required")
    private UUID chapterId;

    private Integer timeSpentSec;
}
