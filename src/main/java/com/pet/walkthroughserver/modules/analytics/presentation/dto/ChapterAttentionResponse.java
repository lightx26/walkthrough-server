package com.pet.walkthroughserver.modules.analytics.presentation.dto;

import java.util.List;
import java.util.UUID;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ChapterAttentionResponse {

    private UUID walkthroughId;
    private List<Chapter> chapters;

    @Getter
    @Builder
    public static class Chapter {
        private UUID chapterId;
        private String chapterTitle;
        private Integer order;
        private Integer totalComments;
        private Boolean allRead;
        private List<AttentionEntry> attention;
    }

    @Getter
    @Builder
    public static class AttentionEntry {
        private UUID userId;
        private String username;
        private String displayName;
        private String avatarUrl;
        private Integer timeSpentSec;
        private Boolean markedAsRead;
        private Integer commentCount;
        private Integer viewCount;
    }
}
