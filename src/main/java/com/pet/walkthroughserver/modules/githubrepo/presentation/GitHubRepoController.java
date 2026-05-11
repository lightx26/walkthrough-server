package com.pet.walkthroughserver.modules.githubrepo.presentation;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.pet.walkthroughserver.interceptors.DataResponse;
import com.pet.walkthroughserver.modules._shared.dto.PageData;
import com.pet.walkthroughserver.modules._shared.infra.github.dto.GitHubRepository;
import com.pet.walkthroughserver.modules.githubrepo.business.services.GitHubRepoService;
import com.pet.walkthroughserver.modules.githubrepo.presentation.dto.RepositoryResponse;
import com.pet.walkthroughserver.modules.githubrepo.presentation.mapper.RepositoryPresentationMapper;
import com.pet.walkthroughserver.modules.pinnedRepo.business.services.PinnedRepoService;
import com.pet.walkthroughserver.modules.walkthrough.business.services.WalkthroughService;
import com.pet.walkthroughserver.security.AuthUser;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/v1/github")
@RequiredArgsConstructor
public class GitHubRepoController {

    private final GitHubRepoService gitHubRepoService;
    private final RepositoryPresentationMapper repositoryMapper;
    private final WalkthroughService walkthroughService;
    private final PinnedRepoService pinnedRepoService;

    @GetMapping("/repos")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<DataResponse<PageData<RepositoryResponse>>> getUserRepositories(
            @AuthenticationPrincipal AuthUser authUser,
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int perPage,
            @RequestParam(defaultValue = "updated") String sort,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String language) {
        UUID userId = UUID.fromString(authUser.getUserId());

        String searchQuery = buildSearchQuery(q, type, language);

        PageData<GitHubRepository> pageData = (searchQuery != null)
                ? gitHubRepoService.searchRepositories(userId, searchQuery, page, perPage)
                : gitHubRepoService.getUserRepositories(userId, page, perPage, sort);
        List<String> fullNames = pageData.getItems().stream()
                .map(GitHubRepository::getFullName)
                .toList();
        Map<String, Long> countMap = walkthroughService.countByRepos(fullNames, userId);
        Set<String> pinnedNames = pinnedRepoService.findPinnedFullNames(userId, fullNames);
        List<RepositoryResponse> responses = pageData.getItems().stream()
                .map(repo -> repositoryMapper.toResponse(repo).toBuilder()
                        .walkthroughsCount(countMap.getOrDefault(repo.getFullName(), 0L))
                        .isPinned(pinnedNames.contains(repo.getFullName()))
                        .build())
                .toList();
        return ResponseEntity.ok(DataResponse.of(
                PageData.of(responses, pageData.getPage(), pageData.getSize(), pageData.getTotalElements(), pageData.getTotalPages())
        ));
    }

    @GetMapping("/repos/{owner}/{repo}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<DataResponse<RepositoryResponse>> getRepository(
            @AuthenticationPrincipal AuthUser authUser,
            @PathVariable String owner,
            @PathVariable String repo) {
        UUID userId = UUID.fromString(authUser.getUserId());
        GitHubRepository repository = gitHubRepoService.getRepository(userId, owner, repo);
        long walkthroughsCount = walkthroughService.countByRepo(owner, repo, userId);
        boolean isPinned = pinnedRepoService.isPinned(userId, owner + "/" + repo);
        RepositoryResponse response = repositoryMapper.toResponse(repository).toBuilder()
                .walkthroughsCount(walkthroughsCount)
                .isPinned(isPinned)
                .build();
        return ResponseEntity.ok(DataResponse.of(response));
    }

    private String buildSearchQuery(String q, String type, String language) {
        boolean hasQuery = q != null && !q.isBlank();
        boolean hasFilters = type != null || language != null;

        if (!hasQuery && !hasFilters) {
            return null;
        }

        StringBuilder sb = new StringBuilder();
        if (hasQuery) {
            sb.append(q);
        }
        if (type != null) {
            sb.append(" is:").append(type);
        }
        if (language != null) {
            sb.append(" language:").append(language);
        }
        return sb.toString().trim();
    }
}
