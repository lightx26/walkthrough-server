package com.pet.walkthroughserver.modules.analytics.presentation.assembler;

import java.util.List;

import org.springframework.stereotype.Component;

import com.pet.walkthroughserver.modules.analytics.business.models.AuthorWalkthroughSummary;
import com.pet.walkthroughserver.modules.analytics.business.models.ChapterAttention;
import com.pet.walkthroughserver.modules.analytics.business.models.RepoMetrics;
import com.pet.walkthroughserver.modules.analytics.business.models.ReviewProgress;
import com.pet.walkthroughserver.modules.analytics.business.models.UnreadSummary;
import com.pet.walkthroughserver.modules.analytics.presentation.dto.AuthorWalkthroughSummaryResponse;
import com.pet.walkthroughserver.modules.analytics.presentation.dto.ChapterAttentionResponse;
import com.pet.walkthroughserver.modules.analytics.presentation.dto.RepoMetricsResponse;
import com.pet.walkthroughserver.modules.analytics.presentation.dto.ReviewProgressResponse;
import com.pet.walkthroughserver.modules.analytics.presentation.dto.UnreadSummaryResponse;

@Component
public class AnalyticsAssembler {

    public ReviewProgressResponse toResponse(ReviewProgress model) {
        return ReviewProgressResponse.builder()
                .walkthroughId(model.walkthroughId())
                .totalChapters(model.totalChapters())
                .reviewers(model.reviewers().stream().map(this::toReviewer).toList())
                .build();
    }

    public ChapterAttentionResponse toResponse(ChapterAttention model) {
        return ChapterAttentionResponse.builder()
                .walkthroughId(model.walkthroughId())
                .chapters(model.chapters().stream().map(this::toChapter).toList())
                .build();
    }

    public UnreadSummaryResponse toResponse(UnreadSummary model) {
        return UnreadSummaryResponse.builder()
                .walkthroughId(model.walkthroughId())
                .allChaptersReadByAll(model.allChaptersReadByAll())
                .unreadChapters(model.unreadChapters().stream().map(this::toUnreadChapter).toList())
                .build();
    }

    public RepoMetricsResponse toResponse(RepoMetrics model) {
        return RepoMetricsResponse.builder()
                .repo(model.repo())
                .period(RepoMetricsResponse.Period.builder()
                        .from(model.period().from())
                        .to(model.period().to())
                        .build())
                .summary(RepoMetricsResponse.Summary.builder()
                        .totalWalkthroughs(model.summary().totalWalkthroughs())
                        .totalReviews(model.summary().totalReviews())
                        .avgCompletionRate(model.summary().avgCompletionRate())
                        .avgTimeToCompleteSec(model.summary().avgTimeToCompleteSec())
                        .avgChaptersPerWalkthrough(model.summary().avgChaptersPerWalkthrough())
                        .activeReviewers(model.summary().activeReviewers())
                        .build())
                .members(model.members().stream().map(this::toMember).toList())
                .trend(model.trend().stream().map(this::toTrendPoint).toList())
                .build();
    }

    public AuthorWalkthroughSummaryResponse toResponse(AuthorWalkthroughSummary model) {
        return AuthorWalkthroughSummaryResponse.builder()
                .walkthroughId(model.walkthroughId())
                .title(model.title())
                .description(model.description())
                .owner(model.owner())
                .repo(model.repo())
                .prNumber(model.prNumber())
                .status(model.status())
                .totalChapters(model.totalChapters())
                .totalComments(model.totalComments())
                .unreadChapterCount(model.unreadChapterCount())
                .lastActivityAt(model.lastActivityAt())
                .reviewers(model.reviewers().stream().map(this::toReviewerStatus).toList())
                .build();
    }

    public List<AuthorWalkthroughSummaryResponse> toResponseList(List<AuthorWalkthroughSummary> models) {
        return models.stream().map(this::toResponse).toList();
    }

    // ── private mappers ──

    private ReviewProgressResponse.Reviewer toReviewer(ReviewProgress.Reviewer r) {
        return ReviewProgressResponse.Reviewer.builder()
                .userId(r.userId())
                .username(r.username())
                .displayName(r.displayName())
                .avatarUrl(r.avatarUrl())
                .readChapters(r.readChapters())
                .totalChapters(r.totalChapters())
                .completionRate(r.completionRate())
                .totalTimeSpentSec(r.totalTimeSpentSec())
                .lastActiveAt(r.lastActiveAt())
                .chapters(r.chapters().stream().map(this::toReviewerChapter).toList())
                .build();
    }

    private ReviewProgressResponse.ReviewerChapter toReviewerChapter(ReviewProgress.ChapterProgress c) {
        return ReviewProgressResponse.ReviewerChapter.builder()
                .chapterId(c.chapterId())
                .chapterTitle(c.chapterTitle())
                .order(c.order())
                .markedAsRead(c.markedAsRead())
                .timeSpentSec(c.timeSpentSec())
                .commentCount(c.commentCount())
                .viewCount(c.viewCount())
                .build();
    }

    private ChapterAttentionResponse.Chapter toChapter(ChapterAttention.Chapter ch) {
        return ChapterAttentionResponse.Chapter.builder()
                .chapterId(ch.chapterId())
                .chapterTitle(ch.chapterTitle())
                .order(ch.order())
                .totalComments(ch.totalComments())
                .allRead(ch.allRead())
                .possiblySkimmedCount(ch.possiblySkimmedCount())
                .attention(ch.attention().stream().map(this::toAttentionEntry).toList())
                .build();
    }

    private ChapterAttentionResponse.AttentionEntry toAttentionEntry(ChapterAttention.AttentionEntry a) {
        return ChapterAttentionResponse.AttentionEntry.builder()
                .userId(a.userId())
                .username(a.username())
                .displayName(a.displayName())
                .avatarUrl(a.avatarUrl())
                .timeSpentSec(a.timeSpentSec())
                .markedAsRead(a.markedAsRead())
                .possiblySkimmed(a.possiblySkimmed())
                .commentCount(a.commentCount())
                .viewCount(a.viewCount())
                .build();
    }

    private UnreadSummaryResponse.UnreadChapter toUnreadChapter(UnreadSummary.UnreadChapter ch) {
        return UnreadSummaryResponse.UnreadChapter.builder()
                .chapterId(ch.chapterId())
                .chapterTitle(ch.chapterTitle())
                .order(ch.order())
                .unreadBy(ch.unreadBy().stream().map(this::toUnreadByUser).toList())
                .build();
    }

    private UnreadSummaryResponse.UnreadByUser toUnreadByUser(UnreadSummary.UnreadByUser u) {
        return UnreadSummaryResponse.UnreadByUser.builder()
                .userId(u.userId())
                .username(u.username())
                .displayName(u.displayName())
                .avatarUrl(u.avatarUrl())
                .build();
    }

    private RepoMetricsResponse.Member toMember(RepoMetrics.Member m) {
        return RepoMetricsResponse.Member.builder()
                .userId(m.userId())
                .username(m.username())
                .displayName(m.displayName())
                .avatarUrl(m.avatarUrl())
                .walkthroughsReviewed(m.walkthroughsReviewed())
                .avgCompletionRate(m.avgCompletionRate())
                .avgTimeSpentSec(m.avgTimeSpentSec())
                .totalComments(m.totalComments())
                .build();
    }

    private RepoMetricsResponse.TrendPoint toTrendPoint(RepoMetrics.TrendPoint t) {
        return RepoMetricsResponse.TrendPoint.builder()
                .week(t.week())
                .avgCompletionRate(t.avgCompletionRate())
                .reviewsCompleted(t.reviewsCompleted())
                .build();
    }

    private AuthorWalkthroughSummaryResponse.ReviewerStatus toReviewerStatus(AuthorWalkthroughSummary.ReviewerStatus r) {
        return AuthorWalkthroughSummaryResponse.ReviewerStatus.builder()
                .userId(r.userId())
                .username(r.username())
                .displayName(r.displayName())
                .avatarUrl(r.avatarUrl())
                .readChapters(r.readChapters())
                .totalChapters(r.totalChapters())
                .lastActiveAt(r.lastActiveAt())
                .build();
    }
}
