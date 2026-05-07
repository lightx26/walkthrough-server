package com.pet.walkthroughserver.modules.starredrepo.presentation;

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
import com.pet.walkthroughserver.modules.starredrepo.business.services.StarredRepoService;
import com.pet.walkthroughserver.modules.starredrepo.presentation.dto.StarRepoRequest;
import com.pet.walkthroughserver.modules.starredrepo.presentation.dto.StarredRepoResponse;
import com.pet.walkthroughserver.modules.starredrepo.repository.StarredRepoEntity;
import com.pet.walkthroughserver.security.AuthUser;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/v1/starred-repos")
@RequiredArgsConstructor
public class StarredRepoController {

    private final StarredRepoService starredRepoService;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<DataResponse<ListData<StarredRepoResponse>>> getStarredRepos(
            @AuthenticationPrincipal AuthUser authUser) {
        UUID userId = UUID.fromString(authUser.getUserId());
        List<StarredRepoEntity> entities = starredRepoService.getStarredRepos(userId);
        List<StarredRepoResponse> responses = entities.stream()
                .map(this::toResponse)
                .toList();
        return ResponseEntity.ok(DataResponse.of(ListData.of(responses)));
    }

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<DataResponse<StarredRepoResponse>> starRepo(
            @AuthenticationPrincipal AuthUser authUser,
            @Valid @RequestBody StarRepoRequest request) {
        UUID userId = UUID.fromString(authUser.getUserId());
        StarredRepoEntity entity = starredRepoService.starRepo(
                userId, request.getRepoFullName(), request.getRepoName(), request.getLanguage());
        return ResponseEntity.ok(DataResponse.of(toResponse(entity)));
    }

    @DeleteMapping("/{owner}/{repo}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<DataResponse<Void>> unstarRepo(
            @AuthenticationPrincipal AuthUser authUser,
            @PathVariable String owner,
            @PathVariable String repo) {
        UUID userId = UUID.fromString(authUser.getUserId());
        String repoFullName = owner + "/" + repo;
        starredRepoService.unstarRepo(userId, repoFullName);
        return ResponseEntity.ok(DataResponse.of(null, "Repository unstarred"));
    }

    @GetMapping("/check/{owner}/{repo}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<DataResponse<Boolean>> isStarred(
            @AuthenticationPrincipal AuthUser authUser,
            @PathVariable String owner,
            @PathVariable String repo) {
        UUID userId = UUID.fromString(authUser.getUserId());
        String repoFullName = owner + "/" + repo;
        boolean starred = starredRepoService.isStarred(userId, repoFullName);
        return ResponseEntity.ok(DataResponse.of(starred));
    }

    private StarredRepoResponse toResponse(StarredRepoEntity entity) {
        return StarredRepoResponse.builder()
                .id(entity.getId())
                .repoFullName(entity.getRepoFullName())
                .repoName(entity.getRepoName())
                .language(entity.getLanguage())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}
