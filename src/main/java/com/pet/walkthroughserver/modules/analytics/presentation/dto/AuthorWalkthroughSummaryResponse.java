package com.pet.walkthroughserver.modules.analytics.presentation.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.pet.walkthroughserver.modules.walkthrough.repository.WalkthroughStatus;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AuthorWalkthroughSummaryResponse {

    private UUID walkthroughId;
    private String title;
    private String description;
    private String owner;
    private String repo;
    private Integer prNumber;
    private WalkthroughStatus status;
    private Integer totalChapters;
    private Integer totalComments;
    private Integer unreadChapterCount;
    private Instant lastActivityAt;
    private List<ReviewerStatus> reviewers;

    @Getter
    @Builder
    public static class ReviewerStatus {
        private UUID userId;
        private String username;
        private String displayName;
        private String avatarUrl;
        private Integer readChapters;
        private Integer totalChapters;
        private Instant lastActiveAt;
    }
}
