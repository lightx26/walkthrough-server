package com.pet.walkthroughserver.modules.walkthrough.presentation;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

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
import com.pet.walkthroughserver.modules.walkthrough.business.models.ReadProgress;
import com.pet.walkthroughserver.modules.walkthrough.business.models.WalkthroughDetail;
import com.pet.walkthroughserver.modules.walkthrough.business.models.WalkthroughSummary;
import com.pet.walkthroughserver.modules.walkthrough.business.services.ReadProgressService;
import com.pet.walkthroughserver.modules.walkthrough.business.services.WalkthroughService;
import com.pet.walkthroughserver.modules.walkthrough.presentation.dto.CreateWalkthroughRequest;
import com.pet.walkthroughserver.modules.walkthrough.presentation.dto.ReadProgressResponse;
import com.pet.walkthroughserver.modules.walkthrough.presentation.dto.RecentlyReviewedResponse;
import com.pet.walkthroughserver.modules.walkthrough.presentation.dto.RecordChapterViewRequest;
import com.pet.walkthroughserver.modules.walkthrough.presentation.dto.UpdateWalkthroughRequest;
import com.pet.walkthroughserver.modules.walkthrough.presentation.dto.WalkthroughResponse;
import com.pet.walkthroughserver.modules.walkthrough.presentation.dto.WalkthroughSummaryResponse;
import com.pet.walkthroughserver.modules.walkthrough.presentation.assembler.WalkthroughAssembler;
import com.pet.walkthroughserver.modules.walkthrough.presentation.mapper.WalkthroughPresentationMapper;
import com.pet.walkthroughserver.modules.walkthrough.repository.ReadProgressEntity;
import com.pet.walkthroughserver.modules.walkthrough.repository.WalkthroughEntity;
import com.pet.walkthroughserver.modules.walkthrough.repository.WalkthroughRepository;
import com.pet.walkthroughserver.security.AuthUser;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/v1/walkthroughs")
@RequiredArgsConstructor
public class WalkthroughController {

    private final WalkthroughService walkthroughService;
    private final ReadProgressService readProgressService;
    private final WalkthroughPresentationMapper walkthroughMapper;
    private final WalkthroughAssembler walkthroughAssembler;
    private final WalkthroughRepository walkthroughRepository;

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
        List<WalkthroughEntity> entities = walkthroughService.listByPr(userId, owner, repo, prNumber);
        List<WalkthroughSummaryResponse> summaries = walkthroughAssembler.toSummaryWithComments(entities);
        return ResponseEntity.ok(DataResponse.of(ListData.of(summaries)));
    }

    @GetMapping("/recent")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<DataResponse<ListData<WalkthroughSummaryResponse>>> listRecent(
            @AuthenticationPrincipal AuthUser authUser) {
        UUID userId = UUID.fromString(authUser.getUserId());
        List<WalkthroughSummary> summaries = walkthroughService.listRecent(userId);
        List<WalkthroughSummaryResponse> responses = walkthroughMapper.toSummaryResponses(summaries);
        return ResponseEntity.ok(DataResponse.of(ListData.of(responses)));
    }

    @GetMapping("/recently-reviewed")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<DataResponse<ListData<RecentlyReviewedResponse>>> listRecentlyReviewed(
            @AuthenticationPrincipal AuthUser authUser) {
        UUID userId = UUID.fromString(authUser.getUserId());
        List<ReadProgressEntity> progressList = readProgressService.listRecentlyReviewed(userId);
        List<UUID> walkthroughIds = progressList.stream()
                .map(ReadProgressEntity::getWalkthroughId).toList();
        Map<UUID, WalkthroughEntity> walkthroughMap = walkthroughRepository.findAllById(walkthroughIds)
                .stream().collect(Collectors.toMap(WalkthroughEntity::getId, Function.identity()));
        List<RecentlyReviewedResponse> responses = progressList.stream()
                .filter(p -> walkthroughMap.containsKey(p.getWalkthroughId()))
                .map(progress -> {
                    WalkthroughEntity wt = walkthroughMap.get(progress.getWalkthroughId());
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
                })
                .toList();
        return ResponseEntity.ok(DataResponse.of(ListData.of(responses)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<DataResponse<WalkthroughResponse>> getById(
            @AuthenticationPrincipal AuthUser authUser,
            @PathVariable UUID id) {
        UUID userId = UUID.fromString(authUser.getUserId());
        WalkthroughDetail detail = walkthroughService.getById(id, userId);
        WalkthroughResponse response = walkthroughMapper.toResponse(detail);
        return ResponseEntity.ok(DataResponse.of(response));
    }

    @PostMapping("/{id}/sync-check")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<DataResponse<WalkthroughResponse>> syncCheck(
            @AuthenticationPrincipal AuthUser authUser,
            @PathVariable UUID id) {
        UUID userId = UUID.fromString(authUser.getUserId());
        WalkthroughEntity entity = walkthroughService.syncCheck(userId, id);
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

    @PostMapping("/{walkthroughId}/chapters/{chapterId}/read")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> markChapterRead(
            @AuthenticationPrincipal AuthUser authUser,
            @PathVariable UUID walkthroughId,
            @PathVariable UUID chapterId) {
        UUID userId = UUID.fromString(authUser.getUserId());
        readProgressService.markChapterRead(userId, walkthroughId, chapterId);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @DeleteMapping("/{walkthroughId}/chapters/{chapterId}/read")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> unmarkChapterRead(
            @AuthenticationPrincipal AuthUser authUser,
            @PathVariable UUID walkthroughId,
            @PathVariable UUID chapterId) {
        UUID userId = UUID.fromString(authUser.getUserId());
        readProgressService.unmarkChapterRead(userId, walkthroughId, chapterId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{walkthroughId}/progress")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<DataResponse<ReadProgressResponse>> getReadProgress(
            @AuthenticationPrincipal AuthUser authUser,
            @PathVariable UUID walkthroughId) {
        UUID userId = UUID.fromString(authUser.getUserId());
        ReadProgress progress = readProgressService.getReadProgress(userId, walkthroughId);
        ReadProgressResponse response = walkthroughAssembler.toProgressResponse(userId, walkthroughId, progress);
        return ResponseEntity.ok(DataResponse.of(response));
    }
}
