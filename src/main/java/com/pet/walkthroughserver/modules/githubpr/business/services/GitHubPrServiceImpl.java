package com.pet.walkthroughserver.modules.githubpr.business.services;

import com.pet.walkthroughserver.modules._shared.infra.github.GitHubResourceClient;
import com.pet.walkthroughserver.modules._shared.infra.github.dto.GitHubCommit;
import com.pet.walkthroughserver.modules._shared.infra.github.dto.GitHubPullRequest;
import com.pet.walkthroughserver.modules._shared.infra.github.dto.GitHubPullRequestFile;
import com.pet.walkthroughserver.modules._shared.infra.github.exceptions.GitHubAccessTokenNotFoundException;
import com.pet.walkthroughserver.modules.user.business.services.UserService;
import com.pet.walkthroughserver.modules.user.repository.UserEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GitHubPrServiceImpl implements GitHubPrService {

    private final GitHubResourceClient gitHubResourceClient;
    private final UserService userService;

    @Override
    public List<GitHubPullRequest> getPullRequests(UUID userId, String owner, String repo,
                                                    String state, int page, int perPage) {
        String accessToken = getGitHubAccessToken(userId);
        return gitHubResourceClient.fetchPullRequests(accessToken, owner, repo, state, page, perPage);
    }

    @Override
    public GitHubPullRequest getPullRequest(UUID userId, String owner, String repo, int pullNumber) {
        String accessToken = getGitHubAccessToken(userId);
        return gitHubResourceClient.fetchPullRequest(accessToken, owner, repo, pullNumber);
    }

    @Override
    public List<GitHubCommit> getPullRequestCommits(UUID userId, String owner, String repo, int pullNumber) {
        String accessToken = getGitHubAccessToken(userId);
        return gitHubResourceClient.fetchPullRequestCommits(accessToken, owner, repo, pullNumber);
    }

    @Override
    public List<GitHubPullRequestFile> getPullRequestFiles(UUID userId, String owner, String repo, int pullNumber) {
        String accessToken = getGitHubAccessToken(userId);
        return gitHubResourceClient.fetchPullRequestFiles(accessToken, owner, repo, pullNumber);
    }

    @Override
    public List<GitHubPullRequestFile> getCommitFiles(UUID userId, String owner, String repo, String commitSha) {
        String accessToken = getGitHubAccessToken(userId);
        return gitHubResourceClient.fetchCommitFiles(accessToken, owner, repo, commitSha);
    }

    @Override
    public Long createPrComment(UUID userId, String owner, String repo, int prNumber, String body) {
        String accessToken = getGitHubAccessToken(userId);
        return gitHubResourceClient.createIssueComment(accessToken, owner, repo, prNumber, body);
    }

    @Override
    public Long createPrReviewComment(UUID userId, String owner, String repo, int prNumber,
                                       String body, String commitId, String path, int position) {
        String accessToken = getGitHubAccessToken(userId);
        return gitHubResourceClient.createPullReviewComment(accessToken, owner, repo, prNumber, body, commitId, path, position);
    }

    @Override
    public List<GitHubPullRequest> getRecentPullRequests(UUID userId, int perPage) {
        UserEntity user = userService.findById(userId);
        String accessToken = getAccessTokenFromUser(user);
        return gitHubResourceClient.searchUserPullRequests(accessToken, user.getUsername(), perPage);
    }

    private String getGitHubAccessToken(UUID userId) {
        return getAccessTokenFromUser(userService.findById(userId));
    }

    private String getAccessTokenFromUser(UserEntity user) {
        String token = user.getGithubAccessToken();
        if (token == null || token.isBlank()) {
            throw new GitHubAccessTokenNotFoundException("GitHub access token not found. Please re-authenticate.");
        }
        return token;
    }
}
