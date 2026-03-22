package com.pet.walkthroughserver.modules.github.business.services;

import com.pet.walkthroughserver.modules.github.exceptions.GitHubAccessTokenNotFoundException;
import com.pet.walkthroughserver.modules._shared.infra.github.GitHubResourceClient;
import com.pet.walkthroughserver.modules._shared.infra.github.dto.GitHubCommit;
import com.pet.walkthroughserver.modules._shared.infra.github.dto.GitHubPullRequest;
import com.pet.walkthroughserver.modules._shared.infra.github.dto.GitHubPullRequestFile;
import com.pet.walkthroughserver.modules._shared.infra.github.dto.GitHubRepository;
import com.pet.walkthroughserver.modules.user.business.services.UserService;
import com.pet.walkthroughserver.modules.user.repository.UserEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GitHubServiceImpl implements GitHubService {

    private final GitHubResourceClient gitHubResourceClient;
    private final UserService userService;

    @Override
    public List<GitHubRepository> getUserRepositories(UUID userId, int page, int perPage, String sort) {
        String accessToken = getGitHubAccessToken(userId);
        return gitHubResourceClient.fetchUserRepositories(accessToken, page, perPage, sort);
    }

    @Override
    public List<GitHubRepository> searchRepositories(UUID userId, String query, int page, int perPage) {
        UserEntity user = userService.findById(userId);
        String accessToken = getAccessTokenFromUser(user);
        String scopedQuery = query + " user:" + user.getUsername();
        return gitHubResourceClient.searchRepositories(accessToken, scopedQuery, page, perPage).getItems();
    }

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
