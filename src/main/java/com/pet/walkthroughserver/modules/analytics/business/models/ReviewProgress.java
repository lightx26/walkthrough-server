package com.pet.walkthroughserver.modules.analytics.business.models;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ReviewProgress(
        UUID walkthroughId,
        int totalChapters,
        List<Reviewer> reviewers
) {
    public record Reviewer(
            UUID userId,
            String username,
            String displayName,
            String avatarUrl,
            int readChapters,
            int totalChapters,
            double completionRate,
            int totalTimeSpentSec,
            Instant lastActiveAt,
            List<ChapterProgress> chapters
    ) {}

    public record ChapterProgress(
            UUID chapterId,
            String chapterTitle,
            int order,
            boolean markedAsRead,
            int timeSpentSec,
            int commentCount,
            int viewCount
    ) {}
}
