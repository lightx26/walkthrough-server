package com.pet.walkthroughserver.modules.walkthrough.presentation.dto;

import java.time.Instant;
import java.util.UUID;

import com.pet.walkthroughserver.modules.walkthrough.repository.WalkthroughStatus;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class WalkthroughSummaryResponse {
    private UUID id;
    private UUID userId;
    private String title;
    private String owner;
    private String repo;
    private Integer prNumber;
    private WalkthroughStatus status;
    private Integer version;
    private Integer chapterCount;
    private Instant createdAt;
    private Instant updatedAt;
}
