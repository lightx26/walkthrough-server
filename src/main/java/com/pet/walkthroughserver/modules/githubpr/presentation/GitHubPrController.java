package com.pet.walkthroughserver.modules.githubpr.presentation;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
import com.pet.walkthroughserver.modules._shared.infra.github.dto.GitHubCommit;
import com.pet.walkthroughserver.modules._shared.infra.github.dto.GitHubPullRequest;
import com.pet.walkthroughserver.modules._shared.infra.github.dto.GitHubPullRequestFile;
import com.pet.walkthroughserver.modules.githubpr.business.services.GitHubPrService;
import com.pet.walkthroughserver.modules.githubpr.presentation.dto.CommitResponse;
import com.pet.walkthroughserver.modules.githubpr.presentation.dto.FileChangeResponse;
import com.pet.walkthroughserver.modules.githubpr.presentation.dto.PullRequestResponse;
import com.pet.walkthroughserver.modules.githubpr.presentation.dto.RecentPullRequestResponse;
import com.pet.walkthroughserver.modules.githubpr.presentation.mapper.CommitPresentationMapper;
import com.pet.walkthroughserver.modules.githubpr.presentation.mapper.FileChangePresentationMapper;
import com.pet.walkthroughserver.modules.githubpr.presentation.mapper.PullRequestPresentationMapper;
import com.pet.walkthroughserver.modules.walkthrough.business.services.WalkthroughService;
import com.pet.walkthroughserver.security.AuthUser;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/v1/github")
@RequiredArgsConstructor
public class GitHubPrController {

    private final GitHubPrService gitHubPrService;
    private final WalkthroughService walkthroughService;
    private final PullRequestPresentationMapper pullRequestMapper;
    private final CommitPresentationMapper commitMapper;
    private final FileChangePresentationMapper fileChangeMapper;

    @GetMapping("/repos/{owner}/{repo}/pulls")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<DataResponse<ListData<PullRequestResponse>>> getPullRequests(
            @AuthenticationPrincipal AuthUser authUser,
            @PathVariable String owner,
            @PathVariable String repo,
            @RequestParam(defaultValue = "open") String state,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "30") int perPage) {
        List<GitHubPullRequest> prs = gitHubPrService.getPullRequests(
                UUID.fromString(authUser.getUserId()), owner, repo, state, page, perPage);
        List<PullRequestResponse> responses = pullRequestMapper.toResponseList(prs);
        List<Integer> prNumbers = responses.stream()
                .map(PullRequestResponse::getNumber)
                .toList();
        Map<Integer, Long> countMap = walkthroughService.countByPrs(owner, repo, prNumbers);
        responses.forEach(pr -> pr.setWalkthroughsCount(
                countMap.getOrDefault(pr.getNumber(), 0L)));
        return ResponseEntity.ok(DataResponse.of(ListData.of(responses)));
    }

    @GetMapping("/repos/{owner}/{repo}/pulls/{pullNumber}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<DataResponse<PullRequestResponse>> getPullRequest(
            @AuthenticationPrincipal AuthUser authUser,
            @PathVariable String owner,
            @PathVariable String repo,
            @PathVariable int pullNumber) {
        GitHubPullRequest pr = gitHubPrService.getPullRequest(
                UUID.fromString(authUser.getUserId()), owner, repo, pullNumber);
        return ResponseEntity.ok(DataResponse.of(pullRequestMapper.toResponse(pr)));
    }

    @GetMapping("/repos/{owner}/{repo}/pulls/{pullNumber}/commits")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<DataResponse<ListData<CommitResponse>>> getPullRequestCommits(
            @AuthenticationPrincipal AuthUser authUser,
            @PathVariable String owner,
            @PathVariable String repo,
            @PathVariable int pullNumber) {
        List<GitHubCommit> commits = gitHubPrService.getPullRequestCommits(
                UUID.fromString(authUser.getUserId()), owner, repo, pullNumber);
        return ResponseEntity.ok(DataResponse.of(ListData.of(commitMapper.toResponseList(commits))));
    }

    @GetMapping("/repos/{owner}/{repo}/pulls/{pullNumber}/files")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<DataResponse<ListData<FileChangeResponse>>> getPullRequestFiles(
            @AuthenticationPrincipal AuthUser authUser,
            @PathVariable String owner,
            @PathVariable String repo,
            @PathVariable int pullNumber) {
        List<GitHubPullRequestFile> files = gitHubPrService.getPullRequestFiles(
                UUID.fromString(authUser.getUserId()), owner, repo, pullNumber);
        return ResponseEntity.ok(DataResponse.of(ListData.of(fileChangeMapper.toResponseList(files))));
    }

    @GetMapping("/repos/{owner}/{repo}/commits/{commitSha}/files")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<DataResponse<ListData<FileChangeResponse>>> getCommitFiles(
            @AuthenticationPrincipal AuthUser authUser,
            @PathVariable String owner,
            @PathVariable String repo,
            @PathVariable String commitSha) {
        List<GitHubPullRequestFile> files = gitHubPrService.getCommitFiles(
                UUID.fromString(authUser.getUserId()), owner, repo, commitSha);
        return ResponseEntity.ok(DataResponse.of(ListData.of(fileChangeMapper.toResponseList(files))));
    }

    @GetMapping("/pulls/recent")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<DataResponse<ListData<RecentPullRequestResponse>>> getRecentPullRequests(
            @AuthenticationPrincipal AuthUser authUser,
            @RequestParam(defaultValue = "10") int perPage) {
        UUID userId = UUID.fromString(authUser.getUserId());
        List<GitHubPullRequest> prs = gitHubPrService.getRecentPullRequests(userId, perPage);
        List<RecentPullRequestResponse> responses = prs.stream()
                .map(this::toRecentPrResponse)
                .toList();
        return ResponseEntity.ok(DataResponse.of(ListData.of(responses)));
    }

    private static final Pattern GITHUB_PR_URL_PATTERN =
            Pattern.compile("https://github\\.com/([^/]+)/([^/]+)/pull/\\d+");

    private RecentPullRequestResponse toRecentPrResponse(GitHubPullRequest pr) {
        String owner = "";
        String repo = "";
        if (pr.getHtmlUrl() != null) {
            Matcher matcher = GITHUB_PR_URL_PATTERN.matcher(pr.getHtmlUrl());
            if (matcher.matches()) {
                owner = matcher.group(1);
                repo = matcher.group(2);
            }
        }

        return RecentPullRequestResponse.builder()
                .id(pr.getId())
                .number(pr.getNumber())
                .title(pr.getTitle())
                .state(pr.getState())
                .htmlUrl(pr.getHtmlUrl())
                .createdAt(pr.getCreatedAt())
                .updatedAt(pr.getUpdatedAt())
                .owner(owner)
                .repo(repo)
                .author(pr.getUser() != null ? RecentPullRequestResponse.Author.builder()
                        .login(pr.getUser().getLogin())
                        .avatarUrl(pr.getUser().getAvatarUrl())
                        .build() : null)
                .build();
    }
}
