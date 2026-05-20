package com.pet.walkthroughserver.modules.analytics.presentation.dto;

import java.util.List;
import java.util.UUID;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class UnreadSummaryResponse {

    private UUID walkthroughId;
    private Boolean allChaptersReadByAll;
    private List<UnreadChapter> unreadChapters;

    @Getter
    @Builder
    public static class UnreadChapter {
        private UUID chapterId;
        private String chapterTitle;
        private Integer order;
        private List<UnreadByUser> unreadBy;
    }

    @Getter
    @Builder
    public static class UnreadByUser {
        private UUID userId;
        private String username;
        private String displayName;
        private String avatarUrl;
    }
}
