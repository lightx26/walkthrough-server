package com.pet.walkthroughserver.modules.profile.presentation.dto;

import com.pet.walkthroughserver.modules.walkthrough.repository.WalkthroughStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Builder
public class ProfileReviewingResponse {

    private UUID walkthroughId;
    private String title;
    private String owner;
    private String repo;
    private int prNumber;
    private WalkthroughStatus status;
    private String creatorDisplayName;
    private String creatorAvatarUrl;
    private int readChapters;
    private int totalChapters;
    private int timeSpentSec;
    private Instant lastReadAt;
}
