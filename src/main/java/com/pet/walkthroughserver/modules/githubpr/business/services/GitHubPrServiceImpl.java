package com.pet.walkthroughserver.modules.githubpr.business.services;

import com.pet.walkthroughserver.configs.CacheNames;
import com.pet.walkthroughserver.modules._shared.infra.github.GitHubResourceClient;
import com.pet.walkthroughserver.modules._shared.infra.github.GitHubTokenProvider;
import com.pet.walkthroughserver.modules._shared.infra.github.dto.GitHubCommit;
import com.pet.walkthroughserver.modules._shared.infra.github.dto.GitHubPullRequest;
import com.pet.walkthroughserver.modules._shared.infra.github.dto.GitHubPullRequestFile;
import com.pet.walkthroughserver.modules.user.business.services.UserService;
import com.pet.walkthroughserver.modules.user.repository.UserEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GitHubPrServiceImpl implements GitHubPrService {

    private final GitHubResourceClient gitHubResourceClient;
    private final GitHubTokenProvider gitHubTokenProvider;
    private final UserService userService;

    @Override
    @Cacheable(value = CacheNames.GITHUB_PULLS, key = "#userId + ':' + #owner + ':' + #repo + ':' + #state + ':' + #page + ':' + #perPage")
    public List<GitHubPullRequest> getPullRequests(UUID userId, String owner, String repo,
                                                    String state, int page, int perPage) {
        String accessToken = gitHubTokenProvider.accessTokenFor(userId);
        return gitHubResourceClient.fetchPullRequests(accessToken, owner, repo, state, page, perPage);
    }

    @Override
    @Cacheable(value = CacheNames.GITHUB_PULL, key = "#userId + ':' + #owner + ':' + #repo + ':' + #pullNumber")
    public GitHubPullRequest getPullRequest(UUID userId, String owner, String repo, int pullNumber) {
        String accessToken = gitHubTokenProvider.accessTokenFor(userId);
        return gitHubResourceClient.fetchPullRequest(accessToken, owner, repo, pullNumber);
    }

    @Override
    @Cacheable(value = CacheNames.GITHUB_PR_COMMITS, key = "#userId + ':' + #owner + ':' + #repo + ':' + #pullNumber")
    public List<GitHubCommit> getPullRequestCommits(UUID userId, String owner, String repo, int pullNumber) {
        String accessToken = gitHubTokenProvider.accessTokenFor(userId);
        return gitHubResourceClient.fetchPullRequestCommits(accessToken, owner, repo, pullNumber);
    }

    @Override
    @Cacheable(value = CacheNames.GITHUB_PR_FILES, key = "#userId + ':' + #owner + ':' + #repo + ':' + #pullNumber")
    public List<GitHubPullRequestFile> getPullRequestFiles(UUID userId, String owner, String repo, int pullNumber) {
        String accessToken = gitHubTokenProvider.accessTokenFor(userId);
        return gitHubResourceClient.fetchPullRequestFiles(accessToken, owner, repo, pullNumber);
    }

    @Override
    public List<GitHubPullRequestFile> getCommitFiles(UUID userId, String owner, String repo, String commitSha) {
        String accessToken = gitHubTokenProvider.accessTokenFor(userId);
        return gitHubResourceClient.fetchCommitFiles(accessToken, owner, repo, commitSha);
    }

    @Override
    @Cacheable(value = CacheNames.GITHUB_RECENT_PULLS, key = "#userId + ':' + #perPage")
    public List<GitHubPullRequest> getRecentPullRequests(UUID userId, int perPage) {
        UserEntity user = userService.findById(userId);
        String accessToken = gitHubTokenProvider.requireToken(user);
        return gitHubResourceClient.searchUserPullRequests(accessToken, user.getUsername(), perPage);
    }

    @Override
    @Cacheable(value = CacheNames.GITHUB_PR_SEARCH, key = "#userId + ':' + #query + ':' + #perPage")
    public List<GitHubPullRequest> searchPullRequests(UUID userId, String query, int perPage) {
        UserEntity user = userService.findById(userId);
        String accessToken = gitHubTokenProvider.requireToken(user);
        return gitHubResourceClient.searchPullRequests(accessToken, query, user.getUsername(), perPage).getItems();
    }
}
