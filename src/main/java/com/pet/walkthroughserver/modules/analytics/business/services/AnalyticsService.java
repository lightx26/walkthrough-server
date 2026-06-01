package com.pet.walkthroughserver.modules.analytics.business.services;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import com.pet.walkthroughserver.modules.analytics.business.models.AuthorWalkthroughSummary;
import com.pet.walkthroughserver.modules.analytics.business.models.ChapterAttention;
import com.pet.walkthroughserver.modules.analytics.business.models.RepoMetrics;
import com.pet.walkthroughserver.modules.analytics.business.models.ReviewProgress;
import com.pet.walkthroughserver.modules.analytics.business.models.UnreadSummary;

public interface AnalyticsService {

    ReviewProgress getReviewProgress(UUID userId, UUID walkthroughId);

    ChapterAttention getChapterAttention(UUID userId, UUID walkthroughId);

    UnreadSummary getUnreadSummary(UUID userId, UUID walkthroughId);

    RepoMetrics getRepoMetrics(UUID userId, String owner, String repo, LocalDate from, LocalDate to);

    List<AuthorWalkthroughSummary> getAuthorSummary(UUID userId, String owner, String repo);
}
