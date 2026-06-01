package com.pet.walkthroughserver.modules.analytics.business.models;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.pet.walkthroughserver.modules.walkthrough.repository.WalkthroughStatus;

public record AuthorWalkthroughSummary(
        UUID walkthroughId,
        String title,
        String description,
        String owner,
        String repo,
        int prNumber,
        WalkthroughStatus status,
        int totalChapters,
        int totalComments,
        int unreadChapterCount,
        Instant lastActivityAt,
        List<ReviewerStatus> reviewers
) {
    public record ReviewerStatus(
            UUID userId,
            String username,
            String displayName,
            String avatarUrl,
            int readChapters,
            int totalChapters,
            Instant lastActiveAt
    ) {}
}
