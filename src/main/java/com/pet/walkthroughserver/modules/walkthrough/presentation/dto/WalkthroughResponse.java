package com.pet.walkthroughserver.modules.walkthrough.presentation.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.pet.walkthroughserver.modules.walkthrough.repository.WalkthroughStatus;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class WalkthroughResponse {
    private UUID id;
    private UUID userId;
    private String creatorUsername;
    private String creatorDisplayName;
    private String creatorAvatarUrl;
    private String title;
    private String description;
    private WalkthroughStatus status;
    private String commitSha;
    private Integer version;
    private String owner;
    private String repo;
    private Integer prNumber;
    private List<ChapterResponse> chapters;
    private Instant createdAt;
    private Instant updatedAt;
}
