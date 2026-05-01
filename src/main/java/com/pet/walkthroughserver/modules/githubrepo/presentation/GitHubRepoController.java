package com.pet.walkthroughserver.modules.githubrepo.presentation;

import com.pet.walkthroughserver.interceptors.DataResponse;
import com.pet.walkthroughserver.modules._shared.dto.ListData;
import com.pet.walkthroughserver.modules._shared.infra.github.dto.GitHubRepository;
import com.pet.walkthroughserver.modules.githubrepo.business.services.GitHubRepoService;
import com.pet.walkthroughserver.modules.githubrepo.presentation.dto.RepositoryResponse;
import com.pet.walkthroughserver.modules.githubrepo.presentation.mapper.RepositoryPresentationMapper;
import com.pet.walkthroughserver.modules.walkthrough.business.services.WalkthroughService;
import com.pet.walkthroughserver.security.AuthUser;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/v1/github")
@RequiredArgsConstructor
public class GitHubRepoController {

    private final GitHubRepoService gitHubRepoService;
    private final RepositoryPresentationMapper repositoryMapper;
    private final WalkthroughService walkthroughService;

    @GetMapping("/repos")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<DataResponse<ListData<RepositoryResponse>>> getUserRepositories(
            @AuthenticationPrincipal AuthUser authUser,
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "30") int perPage,
            @RequestParam(defaultValue = "updated") String sort) {
        UUID userId = UUID.fromString(authUser.getUserId());
        List<GitHubRepository> repos = (q != null && !q.isBlank())
                ? gitHubRepoService.searchRepositories(userId, q, page, perPage)
                : gitHubRepoService.getUserRepositories(userId, page, perPage, sort);
        List<RepositoryResponse> responses = repos.stream()
                .map(repo -> {
                    String[] parts = repo.getFullName().split("/", 2);
                    long walkthroughsCount = parts.length == 2
                            ? walkthroughService.countByRepo(parts[0], parts[1])
                            : 0L;
                    return repositoryMapper.toResponse(repo).toBuilder()
                            .walkthroughsCount(walkthroughsCount)
                            .build();
                })
                .toList();
        return ResponseEntity.ok(DataResponse.of(ListData.of(responses)));
    }
}
