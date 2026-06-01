package com.pet.walkthroughserver.modules.analytics.business.models;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record RepoMetrics(
        String repo,
        Period period,
        Summary summary,
        List<Member> members,
        List<TrendPoint> trend
) {
    public record Period(LocalDate from, LocalDate to) {}

    public record Summary(
            int totalWalkthroughs,
            int totalReviews,
            double avgCompletionRate,
            int avgTimeToCompleteSec,
            double avgChaptersPerWalkthrough,
            int activeReviewers
    ) {}

    public record Member(
            UUID userId,
            String username,
            String displayName,
            String avatarUrl,
            int walkthroughsReviewed,
            double avgCompletionRate,
            int avgTimeSpentSec,
            int totalComments
    ) {}

    public record TrendPoint(
            String week,
            double avgCompletionRate,
            int reviewsCompleted
    ) {}
}
