package com.pet.walkthroughserver.modules.walkthrough.presentation.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Builder
public class WalkthroughSummaryResponse {
    private UUID id;
    private String title;
    private String status;
    private Integer chapterCount;
    private Instant createdAt;
    private Instant updatedAt;
}
