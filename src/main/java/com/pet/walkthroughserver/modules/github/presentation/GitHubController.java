package com.pet.walkthroughserver.modules.github.presentation;

import com.pet.walkthroughserver.modules._shared.dto.DataResponse;
import com.pet.walkthroughserver.modules._shared.dto.ListData;
import com.pet.walkthroughserver.modules._shared.infra.github.dto.GitHubPullRequest;
import com.pet.walkthroughserver.modules._shared.infra.github.dto.GitHubRepository;
import com.pet.walkthroughserver.modules.github.business.services.GitHubService;
import com.pet.walkthroughserver.modules.github.presentation.dto.PullRequestResponse;
import com.pet.walkthroughserver.modules.github.presentation.dto.RepositoryResponse;
import com.pet.walkthroughserver.modules.github.presentation.mapper.PullRequestPresentationMapper;
import com.pet.walkthroughserver.modules.github.presentation.mapper.RepositoryPresentationMapper;
import com.pet.walkthroughserver.security.AuthUser;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/v1/github")
@RequiredArgsConstructor
public class GitHubController {

    private final GitHubService gitHubService;
    private final RepositoryPresentationMapper repositoryMapper;
    private final PullRequestPresentationMapper pullRequestMapper;

    @GetMapping("/repos")
    public ResponseEntity<DataResponse<ListData<RepositoryResponse>>> getUserRepositories(
            @AuthenticationPrincipal AuthUser authUser,
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "30") int perPage,
            @RequestParam(defaultValue = "updated") String sort) {
        UUID userId = UUID.fromString(authUser.getUserId());
        List<GitHubRepository> repos = (q != null && !q.isBlank())
                ? gitHubService.searchRepositories(userId, q, page, perPage)
                : gitHubService.getUserRepositories(userId, page, perPage, sort);
        return ResponseEntity.ok(DataResponse.of(ListData.of(repositoryMapper.toResponseList(repos))));
    }

    @GetMapping("/repos/{owner}/{repo}/pulls")
    public ResponseEntity<DataResponse<ListData<PullRequestResponse>>> getPullRequests(
            @AuthenticationPrincipal AuthUser authUser,
            @PathVariable String owner,
            @PathVariable String repo,
            @RequestParam(defaultValue = "open") String state,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "30") int perPage) {
        List<GitHubPullRequest> prs = gitHubService.getPullRequests(
                UUID.fromString(authUser.getUserId()), owner, repo, state, page, perPage);
        return ResponseEntity.ok(DataResponse.of(ListData.of(pullRequestMapper.toResponseList(prs))));
    }

    @GetMapping("/repos/{owner}/{repo}/pulls/{pullNumber}")
    public ResponseEntity<DataResponse<PullRequestResponse>> getPullRequest(
            @AuthenticationPrincipal AuthUser authUser,
            @PathVariable String owner,
            @PathVariable String repo,
            @PathVariable int pullNumber) {
        GitHubPullRequest pr = gitHubService.getPullRequest(
                UUID.fromString(authUser.getUserId()), owner, repo, pullNumber);
        return ResponseEntity.ok(DataResponse.of(pullRequestMapper.toResponse(pr)));
    }
}
