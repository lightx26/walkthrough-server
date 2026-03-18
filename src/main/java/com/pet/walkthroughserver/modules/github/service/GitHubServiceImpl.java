package com.pet.walkthroughserver.modules.github.service;

import com.pet.walkthroughserver.exceptions.AppException;
import com.pet.walkthroughserver.modules.common.github.GitHubClient;
import com.pet.walkthroughserver.modules.common.github.dto.GitHubPullRequest;
import com.pet.walkthroughserver.modules.common.github.dto.GitHubRepository;
import com.pet.walkthroughserver.modules.user.entity.UserEntity;
import com.pet.walkthroughserver.modules.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GitHubServiceImpl implements GitHubService {

    private final GitHubClient gitHubClient;
    private final UserService userService;

    @Override
    public List<GitHubRepository> getUserRepositories(UUID userId, int page, int perPage, String sort) {
        String accessToken = getGitHubAccessToken(userId);
        return gitHubClient.fetchUserRepositories(accessToken, page, perPage, sort);
    }

    @Override
    public List<GitHubRepository> searchRepositories(UUID userId, String query, int page, int perPage) {
        UserEntity user = userService.findById(userId);
        String accessToken = getAccessTokenFromUser(user);
        // Scope the search to the authenticated user's repos
        String scopedQuery = query + " user:" + user.getUsername();
        return gitHubClient.searchRepositories(accessToken, scopedQuery, page, perPage).getItems();
    }

    @Override
    public List<GitHubPullRequest> getPullRequests(UUID userId, String owner, String repo,
                                                    String state, int page, int perPage) {
        String accessToken = getGitHubAccessToken(userId);
        return gitHubClient.fetchPullRequests(accessToken, owner, repo, state, page, perPage);
    }

    @Override
    public GitHubPullRequest getPullRequest(UUID userId, String owner, String repo, int pullNumber) {
        String accessToken = getGitHubAccessToken(userId);
        return gitHubClient.fetchPullRequest(accessToken, owner, repo, pullNumber);
    }

    private String getGitHubAccessToken(UUID userId) {
        return getAccessTokenFromUser(userService.findById(userId));
    }

    private String getAccessTokenFromUser(UserEntity user) {
        String token = user.getGithubAccessToken();
        if (token == null || token.isBlank()) {
            throw AppException.unauthorized("GitHub access token not found. Please re-authenticate.");
        }
        return token;
    }
}
