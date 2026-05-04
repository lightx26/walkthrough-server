package com.pet.walkthroughserver.modules.walkthrough.presentation;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.pet.walkthroughserver.interceptors.DataResponse;
import com.pet.walkthroughserver.modules._shared.dto.ListData;
import com.pet.walkthroughserver.modules.walkthrough.business.services.ReadProgressService;
import com.pet.walkthroughserver.modules.walkthrough.business.services.WalkthroughService;
import com.pet.walkthroughserver.modules.walkthrough.business.services.WalkthroughVersionService;
import com.pet.walkthroughserver.modules.walkthrough.presentation.dto.CreateWalkthroughRequest;
import com.pet.walkthroughserver.modules.walkthrough.presentation.dto.ReadProgressResponse;
import com.pet.walkthroughserver.modules.walkthrough.presentation.dto.RecentlyReviewedResponse;
import com.pet.walkthroughserver.modules.walkthrough.presentation.dto.RecordChapterViewRequest;
import com.pet.walkthroughserver.modules.walkthrough.presentation.dto.StalenessResponse;
import com.pet.walkthroughserver.modules.walkthrough.presentation.dto.UpdateWalkthroughRequest;
import com.pet.walkthroughserver.modules.walkthrough.presentation.dto.VersionDiffResponse;
import com.pet.walkthroughserver.modules.walkthrough.presentation.dto.WalkthroughResponse;
import com.pet.walkthroughserver.modules.walkthrough.presentation.dto.WalkthroughSummaryResponse;
import com.pet.walkthroughserver.modules.walkthrough.presentation.mapper.ReadProgressPresentationMapper;
import com.pet.walkthroughserver.modules.walkthrough.presentation.mapper.WalkthroughPresentationMapper;
import com.pet.walkthroughserver.modules.walkthrough.repository.ReadProgressEntity;
import com.pet.walkthroughserver.modules.walkthrough.repository.WalkthroughEntity;
import com.pet.walkthroughserver.security.AuthUser;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/v1/walkthroughs")
@RequiredArgsConstructor
public class WalkthroughController {

    private final WalkthroughService walkthroughService;
    private final ReadProgressService readProgressService;
    private final WalkthroughVersionService walkthroughVersionService;
    private final WalkthroughPresentationMapper walkthroughMapper;
    private final ReadProgressPresentationMapper readProgressMapper;

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<DataResponse<WalkthroughResponse>> create(
            @AuthenticationPrincipal AuthUser authUser,
            @Valid @RequestBody CreateWalkthroughRequest request) {
        UUID userId = UUID.fromString(authUser.getUserId());
        WalkthroughEntity entity = walkthroughService.create(userId, authUser.getUsername(), request);
        WalkthroughResponse response = walkthroughMapper.toResponse(entity);
        return ResponseEntity.status(HttpStatus.CREATED).body(DataResponse.of(response));
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<DataResponse<ListData<WalkthroughSummaryResponse>>> list(
            @AuthenticationPrincipal AuthUser authUser,
            @RequestParam String owner,
            @RequestParam String repo,
            @RequestParam Integer prNumber) {
        UUID userId = UUID.fromString(authUser.getUserId());
        List<WalkthroughEntity> entities = walkthroughService.listByPr(owner, repo, prNumber, userId);
        List<WalkthroughSummaryResponse> summaries = walkthroughMapper.toSummaryResponseList(entities);
        List<UUID> ids = entities.stream().map(WalkthroughEntity::getId).toList();
        var commentCounts = walkthroughService.getCommentCounts(ids);
        summaries.forEach(s -> s.setCommentCount(commentCounts.getOrDefault(s.getId(), 0L).intValue()));
        return ResponseEntity.ok(DataResponse.of(ListData.of(summaries)));
    }

    @GetMapping("/recent")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<DataResponse<ListData<WalkthroughSummaryResponse>>> listRecent(
            @AuthenticationPrincipal AuthUser authUser) {
        UUID userId = UUID.fromString(authUser.getUserId());
        List<WalkthroughEntity> entities = walkthroughService.listRecent(userId);
        List<WalkthroughSummaryResponse> summaries = walkthroughMapper.toSummaryResponseList(entities);
        return ResponseEntity.ok(DataResponse.of(ListData.of(summaries)));
    }

    @GetMapping("/recently-reviewed")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<DataResponse<ListData<RecentlyReviewedResponse>>> listRecentlyReviewed(
            @AuthenticationPrincipal AuthUser authUser) {
        UUID userId = UUID.fromString(authUser.getUserId());
        List<ReadProgressEntity> progressList = readProgressService.listRecentlyReviewed(userId);
        List<RecentlyReviewedResponse> responses = progressList.stream()
                .map(progress -> {
                    try {
                        WalkthroughEntity wt = walkthroughService.getById(progress.getWalkthroughId(), userId);
                        return RecentlyReviewedResponse.builder()
                                .walkthroughId(wt.getId())
                                .title(wt.getTitle())
                                .owner(wt.getOwner())
                                .repo(wt.getRepo())
                                .prNumber(wt.getPrNumber())
                                .status(wt.getStatus())
                                .readChapters(progress.getReadChapters())
                                .totalChapters(progress.getTotalChapters())
                                .timeSpentSec(progress.getTimeSpentSec())
                                .lastReadAt(progress.getReadAt())
                                .build();
                    } catch (Exception e) {
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .toList();
        return ResponseEntity.ok(DataResponse.of(ListData.of(responses)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<DataResponse<WalkthroughResponse>> getById(
            @AuthenticationPrincipal AuthUser authUser,
            @PathVariable UUID id) {
        UUID userId = UUID.fromString(authUser.getUserId());
        WalkthroughEntity entity = walkthroughService.getById(id, userId);
        WalkthroughResponse response = walkthroughMapper.toResponse(entity);
        return ResponseEntity.ok(DataResponse.of(response));
    }

    @PutMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<DataResponse<WalkthroughResponse>> update(
            @AuthenticationPrincipal AuthUser authUser,
            @PathVariable UUID id,
            @Valid @RequestBody UpdateWalkthroughRequest request) {
        UUID userId = UUID.fromString(authUser.getUserId());
        WalkthroughEntity entity = walkthroughService.update(userId, id, request);
        WalkthroughResponse response = walkthroughMapper.toResponse(entity);
        return ResponseEntity.ok(DataResponse.of(response));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> delete(
            @AuthenticationPrincipal AuthUser authUser,
            @PathVariable UUID id) {
        UUID userId = UUID.fromString(authUser.getUserId());
        walkthroughService.delete(userId, id);
        return ResponseEntity.noContent().build();
    }

    // ── Reading Progress endpoints ──

    @PostMapping("/{walkthroughId}/chapter-view-events")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> recordChapterView(
            @AuthenticationPrincipal AuthUser authUser,
            @PathVariable UUID walkthroughId,
            @Valid @RequestBody RecordChapterViewRequest request) {
        UUID userId = UUID.fromString(authUser.getUserId());
        readProgressService.recordChapterView(userId, walkthroughId, request);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @GetMapping("/{walkthroughId}/progress")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<DataResponse<ReadProgressResponse>> getReadProgress(
            @AuthenticationPrincipal AuthUser authUser,
            @PathVariable UUID walkthroughId) {
        UUID userId = UUID.fromString(authUser.getUserId());
        ReadProgressEntity entity = readProgressService.getReadProgress(userId, walkthroughId);
        return ResponseEntity.ok(DataResponse.of(readProgressMapper.toResponse(entity)));
    }

    // ── Versioning endpoints ──

    @GetMapping("/{id}/staleness")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<DataResponse<StalenessResponse>> checkStaleness(
            @AuthenticationPrincipal AuthUser authUser,
            @PathVariable UUID id) {
        UUID userId = UUID.fromString(authUser.getUserId());
        StalenessResponse response = walkthroughVersionService.checkStaleness(userId, id);
        return ResponseEntity.ok(DataResponse.of(response));
    }

    @PostMapping("/{id}/new-version")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<DataResponse<WalkthroughResponse>> createNewVersion(
            @AuthenticationPrincipal AuthUser authUser,
            @PathVariable UUID id) {
        UUID userId = UUID.fromString(authUser.getUserId());
        WalkthroughEntity entity = walkthroughVersionService.createNewVersion(userId, id);
        WalkthroughResponse response = walkthroughMapper.toResponse(entity);
        return ResponseEntity.status(HttpStatus.CREATED).body(DataResponse.of(response));
    }

    @GetMapping("/{id}/diff")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<DataResponse<VersionDiffResponse>> getVersionDiff(
            @AuthenticationPrincipal AuthUser authUser,
            @PathVariable UUID id,
            @RequestParam int fromVersion,
            @RequestParam int toVersion) {
        UUID userId = UUID.fromString(authUser.getUserId());
        VersionDiffResponse response = walkthroughVersionService.getVersionDiff(userId, id, fromVersion, toVersion);
        return ResponseEntity.ok(DataResponse.of(response));
    }

}

