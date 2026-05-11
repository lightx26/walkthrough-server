package com.pet.walkthroughserver.modules.pinnedRepo.presentation;

import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.pet.walkthroughserver.interceptors.DataResponse;
import com.pet.walkthroughserver.modules._shared.dto.ListData;
import com.pet.walkthroughserver.modules.pinnedRepo.business.services.PinnedRepoService;
import com.pet.walkthroughserver.modules.pinnedRepo.presentation.dto.PinnedRepoRequest;
import com.pet.walkthroughserver.modules.pinnedRepo.presentation.dto.PinnedRepoResponse;
import com.pet.walkthroughserver.modules.pinnedRepo.repository.PinnedRepoEntity;
import com.pet.walkthroughserver.security.AuthUser;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/v1/pinned-repos")
@RequiredArgsConstructor
public class PinnedRepoController {

    private final PinnedRepoService pinnedRepoService;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<DataResponse<ListData<PinnedRepoResponse>>> getPinnedRepos(
            @AuthenticationPrincipal AuthUser authUser) {
        UUID userId = UUID.fromString(authUser.getUserId());
        List<PinnedRepoEntity> entities = pinnedRepoService.getPinnedRepos(userId);
        List<PinnedRepoResponse> responses = entities.stream()
                .map(this::toResponse)
                .toList();
        return ResponseEntity.ok(DataResponse.of(ListData.of(responses)));
    }

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<DataResponse<PinnedRepoResponse>> pinRepo(
            @AuthenticationPrincipal AuthUser authUser,
            @Valid @RequestBody PinnedRepoRequest request) {
        UUID userId = UUID.fromString(authUser.getUserId());
        PinnedRepoEntity entity = pinnedRepoService.pinRepo(
                userId, request.getRepoFullName(), request.getRepoName(), request.getLanguage());
        return ResponseEntity.ok(DataResponse.of(toResponse(entity)));
    }

    @DeleteMapping("/{owner}/{repo}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<DataResponse<Void>> unpinRepo(
            @AuthenticationPrincipal AuthUser authUser,
            @PathVariable String owner,
            @PathVariable String repo) {
        UUID userId = UUID.fromString(authUser.getUserId());
        String repoFullName = owner + "/" + repo;
        pinnedRepoService.unpinRepo(userId, repoFullName);
        return ResponseEntity.ok(DataResponse.of(null, "Repository unpinned"));
    }

    @GetMapping("/check/{owner}/{repo}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<DataResponse<Boolean>> isPinned(
            @AuthenticationPrincipal AuthUser authUser,
            @PathVariable String owner,
            @PathVariable String repo) {
        UUID userId = UUID.fromString(authUser.getUserId());
        String repoFullName = owner + "/" + repo;
        boolean pinned = pinnedRepoService.isPinned(userId, repoFullName);
        return ResponseEntity.ok(DataResponse.of(pinned));
    }

    private PinnedRepoResponse toResponse(PinnedRepoEntity entity) {
        return PinnedRepoResponse.builder()
                .id(entity.getId())
                .repoFullName(entity.getRepoFullName())
                .repoName(entity.getRepoName())
                .language(entity.getLanguage())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}
