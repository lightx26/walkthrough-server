package com.pet.walkthroughserver.modules.analytics.business.models;

import java.util.List;
import java.util.UUID;

public record UnreadSummary(
        UUID walkthroughId,
        boolean allChaptersReadByAll,
        List<UnreadChapter> unreadChapters
) {
    public record UnreadChapter(
            UUID chapterId,
            String chapterTitle,
            int order,
            List<UnreadByUser> unreadBy
    ) {}

    public record UnreadByUser(
            UUID userId,
            String username,
            String displayName,
            String avatarUrl
    ) {}
}
