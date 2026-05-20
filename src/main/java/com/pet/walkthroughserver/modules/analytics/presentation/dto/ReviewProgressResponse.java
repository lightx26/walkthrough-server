package com.pet.walkthroughserver.modules.analytics.presentation.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ReviewProgressResponse {

    private UUID walkthroughId;
    private Integer totalChapters;
    private List<Reviewer> reviewers;

    @Getter
    @Builder
    public static class Reviewer {
        private UUID userId;
        private String username;
        private String displayName;
        private String avatarUrl;
        private Integer readChapters;
        private Integer totalChapters;
        private Double completionRate;
        private Integer totalTimeSpentSec;
        private Instant lastActiveAt;
        private List<ReviewerChapter> chapters;
    }

    @Getter
    @Builder
    public static class ReviewerChapter {
        private UUID chapterId;
        private String chapterTitle;
        private Integer order;
        private Boolean read;
        private Boolean scrolledToBottom;
        private Integer timeSpentSec;
        private Integer commentCount;
        private Integer viewCount;
    }
}
