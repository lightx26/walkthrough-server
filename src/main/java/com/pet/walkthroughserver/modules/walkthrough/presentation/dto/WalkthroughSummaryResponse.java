package com.pet.walkthroughserver.modules.walkthrough.presentation.dto;

import java.time.Instant;
import java.util.UUID;

import com.pet.walkthroughserver.modules.walkthrough.repository.WalkthroughStatus;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Builder
public class WalkthroughSummaryResponse {
    private UUID id;
    private UUID userId;
    private String title;
    private String description;
    private String owner;
    private String repo;
    private Integer prNumber;
    private WalkthroughStatus status;
    private String outdatedReason;
    private Integer chapterCount;
    @Setter
    private Integer commentCount;
    private Instant createdAt;
    private Instant updatedAt;
}
