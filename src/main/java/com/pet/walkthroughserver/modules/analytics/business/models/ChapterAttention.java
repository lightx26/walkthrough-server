package com.pet.walkthroughserver.modules.analytics.business.models;

import java.util.List;
import java.util.UUID;

public record ChapterAttention(
        UUID walkthroughId,
        List<Chapter> chapters
) {
    public record Chapter(
            UUID chapterId,
            String chapterTitle,
            int order,
            int totalComments,
            boolean allRead,
            int possiblySkimmedCount,
            List<AttentionEntry> attention
    ) {}

    public record AttentionEntry(
            UUID userId,
            String username,
            String displayName,
            String avatarUrl,
            int timeSpentSec,
            boolean markedAsRead,
            boolean possiblySkimmed,
            int commentCount,
            int viewCount
    ) {}
}
