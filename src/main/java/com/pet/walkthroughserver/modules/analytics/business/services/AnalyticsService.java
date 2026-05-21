package com.pet.walkthroughserver.modules.analytics.business.services;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import com.pet.walkthroughserver.modules.analytics.presentation.dto.AuthorWalkthroughSummaryResponse;
import com.pet.walkthroughserver.modules.analytics.presentation.dto.ChapterAttentionResponse;
import com.pet.walkthroughserver.modules.analytics.presentation.dto.RepoMetricsResponse;
import com.pet.walkthroughserver.modules.analytics.presentation.dto.ReviewProgressResponse;
import com.pet.walkthroughserver.modules.analytics.presentation.dto.UnreadSummaryResponse;

public interface AnalyticsService {

    ReviewProgressResponse getReviewProgress(UUID userId, UUID walkthroughId);

    ChapterAttentionResponse getChapterAttention(UUID userId, UUID walkthroughId);

    UnreadSummaryResponse getUnreadSummary(UUID userId, UUID walkthroughId);

    RepoMetricsResponse getRepoMetrics(UUID userId, String owner, String repo, LocalDate from, LocalDate to);

    List<AuthorWalkthroughSummaryResponse> getAuthorSummary(UUID userId, String owner, String repo);
}
