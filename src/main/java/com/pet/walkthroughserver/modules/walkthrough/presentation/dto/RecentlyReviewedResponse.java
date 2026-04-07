package com.pet.walkthroughserver.modules.walkthrough.presentation.dto;

import com.pet.walkthroughserver.modules.walkthrough.repository.WalkthroughStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Builder
public class RecentlyReviewedResponse {
    private UUID walkthroughId;
    private String title;
    private String owner;
    private String repo;
    private Integer prNumber;
    private WalkthroughStatus status;
    private Integer readChapters;
    private Integer totalChapters;
    private Integer timeSpentSec;
    private Instant lastReadAt;
}
