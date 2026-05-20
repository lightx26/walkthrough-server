package com.pet.walkthroughserver.modules.analytics.presentation.dto;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class RepoMetricsResponse {

    private String repo;
    private Period period;
    private Summary summary;
    private List<Member> members;
    private List<TrendPoint> trend;

    @Getter
    @Builder
    public static class Period {
        private LocalDate from;
        private LocalDate to;
    }

    @Getter
    @Builder
    public static class Summary {
        private Integer totalWalkthroughs;
        private Integer totalReviews;
        private Double avgCompletionRate;
        private Integer avgTimeToCompleteSec;
        private Double avgChaptersPerWalkthrough;
        private Integer activeReviewers;
    }

    @Getter
    @Builder
    public static class Member {
        private UUID userId;
        private String username;
        private String displayName;
        private String avatarUrl;
        private Integer walkthroughsReviewed;
        private Double avgCompletionRate;
        private Integer avgTimeSpentSec;
        private Integer totalComments;
    }

    @Getter
    @Builder
    public static class TrendPoint {
        private String week;
        private Double avgCompletionRate;
        private Integer reviewsCompleted;
    }
}
