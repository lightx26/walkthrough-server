package com.pet.walkthroughserver.modules.analytics.presentation;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.pet.walkthroughserver.interceptors.DataResponse;
import com.pet.walkthroughserver.modules._shared.dto.ListData;
import com.pet.walkthroughserver.modules.analytics.business.services.AnalyticsService;
import com.pet.walkthroughserver.modules.analytics.presentation.assembler.AnalyticsAssembler;
import com.pet.walkthroughserver.modules.analytics.presentation.dto.AuthorWalkthroughSummaryResponse;
import com.pet.walkthroughserver.modules.analytics.presentation.dto.ChapterAttentionResponse;
import com.pet.walkthroughserver.modules.analytics.presentation.dto.RepoMetricsResponse;
import com.pet.walkthroughserver.modules.analytics.presentation.dto.ReviewProgressResponse;
import com.pet.walkthroughserver.modules.analytics.presentation.dto.UnreadSummaryResponse;
import com.pet.walkthroughserver.security.AuthUser;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/v1/analytics")
@RequiredArgsConstructor
public class AnalyticsController {

    private final AnalyticsService analyticsService;
    private final AnalyticsAssembler analyticsAssembler;

    // 6.1 — Per-reviewer reading matrix
    @GetMapping("/walkthroughs/{id}/review-progress")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<DataResponse<ReviewProgressResponse>> reviewProgress(
            @AuthenticationPrincipal AuthUser authUser,
            @PathVariable UUID id) {
        UUID userId = UUID.fromString(authUser.getUserId());
        ReviewProgressResponse response = analyticsAssembler.toResponse(
                analyticsService.getReviewProgress(userId, id));
        return ResponseEntity.ok(DataResponse.of(response));
    }

    // 6.2 — Chapter attention
    @GetMapping("/walkthroughs/{id}/chapter-attention")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<DataResponse<ChapterAttentionResponse>> chapterAttention(
            @AuthenticationPrincipal AuthUser authUser,
            @PathVariable UUID id) {
        UUID userId = UUID.fromString(authUser.getUserId());
        ChapterAttentionResponse response = analyticsAssembler.toResponse(
                analyticsService.getChapterAttention(userId, id));
        return ResponseEntity.ok(DataResponse.of(response));
    }

    // 6.3 — Unread summary
    @GetMapping("/walkthroughs/{id}/unread-summary")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<DataResponse<UnreadSummaryResponse>> unreadSummary(
            @AuthenticationPrincipal AuthUser authUser,
            @PathVariable UUID id) {
        UUID userId = UUID.fromString(authUser.getUserId());
        UnreadSummaryResponse response = analyticsAssembler.toResponse(
                analyticsService.getUnreadSummary(userId, id));
        return ResponseEntity.ok(DataResponse.of(response));
    }

    // 6.4 — Repo-level longitudinal metrics
    @GetMapping("/repos/{owner}/{repo}/metrics")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<DataResponse<RepoMetricsResponse>> repoMetrics(
            @AuthenticationPrincipal AuthUser authUser,
            @PathVariable String owner,
            @PathVariable String repo,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        UUID userId = UUID.fromString(authUser.getUserId());
        RepoMetricsResponse response = analyticsAssembler.toResponse(
                analyticsService.getRepoMetrics(userId, owner, repo, from, to));
        return ResponseEntity.ok(DataResponse.of(response));
    }

    // Author summary list (feeds the Author view in the analytics index page)
    @GetMapping("/author-summary")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<DataResponse<ListData<AuthorWalkthroughSummaryResponse>>> authorSummary(
            @AuthenticationPrincipal AuthUser authUser,
            @RequestParam(required = false) String owner,
            @RequestParam(required = false) String repo) {
        UUID userId = UUID.fromString(authUser.getUserId());
        List<AuthorWalkthroughSummaryResponse> list = analyticsAssembler.toResponseList(
                analyticsService.getAuthorSummary(userId, owner, repo));
        return ResponseEntity.ok(DataResponse.of(ListData.of(list)));
    }
}
