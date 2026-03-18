package com.pet.walkthroughserver.modules.github.controller;

import com.pet.walkthroughserver.modules.common.dto.ApiResponse;
import com.pet.walkthroughserver.modules.common.github.dto.GitHubPullRequest;
import com.pet.walkthroughserver.modules.common.github.dto.GitHubRepository;
import com.pet.walkthroughserver.modules.github.service.GitHubService;
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

    @GetMapping("/repos")
    public ResponseEntity<ApiResponse<List<GitHubRepository>>> getUserRepositories(
            @AuthenticationPrincipal AuthUser authUser,
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "30") int perPage,
            @RequestParam(defaultValue = "updated") String sort) {
        UUID userId = UUID.fromString(authUser.getUserId());
        List<GitHubRepository> repos = (q != null && !q.isBlank())
                ? gitHubService.searchRepositories(userId, q, page, perPage)
                : gitHubService.getUserRepositories(userId, page, perPage, sort);
        return ResponseEntity.ok(ApiResponse.ok(repos));
    }

    @GetMapping("/repos/{owner}/{repo}/pulls")
    public ResponseEntity<ApiResponse<List<GitHubPullRequest>>> getPullRequests(
            @AuthenticationPrincipal AuthUser authUser,
            @PathVariable String owner,
            @PathVariable String repo,
            @RequestParam(defaultValue = "open") String state,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "30") int perPage) {
        List<GitHubPullRequest> prs = gitHubService.getPullRequests(
                UUID.fromString(authUser.getUserId()), owner, repo, state, page, perPage);
        return ResponseEntity.ok(ApiResponse.ok(prs));
    }

    @GetMapping("/repos/{owner}/{repo}/pulls/{pullNumber}")
    public ResponseEntity<ApiResponse<GitHubPullRequest>> getPullRequest(
            @AuthenticationPrincipal AuthUser authUser,
            @PathVariable String owner,
            @PathVariable String repo,
            @PathVariable int pullNumber) {
        GitHubPullRequest pr = gitHubService.getPullRequest(
                UUID.fromString(authUser.getUserId()), owner, repo, pullNumber);
        return ResponseEntity.ok(ApiResponse.ok(pr));
    }
}
