package com.pet.walkthroughserver.modules.github.service;

import com.pet.walkthroughserver.exceptions.AppException;
import com.pet.walkthroughserver.modules._shared.infra.github.GitHubResourceClient;
import com.pet.walkthroughserver.modules.github.dto.PullRequestResponse;
import com.pet.walkthroughserver.modules.github.dto.RepositoryResponse;
import com.pet.walkthroughserver.modules.github.mapper.PullRequestMapper;
import com.pet.walkthroughserver.modules.github.mapper.RepositoryMapper;
import com.pet.walkthroughserver.modules.user.entity.UserEntity;
import com.pet.walkthroughserver.modules.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GitHubServiceImpl implements GitHubService {

    private final GitHubResourceClient gitHubResourceClient;
    private final UserService userService;
    private final RepositoryMapper repositoryMapper;
    private final PullRequestMapper pullRequestMapper;

    @Override
    public List<RepositoryResponse> getUserRepositories(UUID userId, int page, int perPage, String sort) {
        String accessToken = getGitHubAccessToken(userId);
        return repositoryMapper.toResponseList(gitHubResourceClient.fetchUserRepositories(accessToken, page, perPage, sort));
    }

    @Override
    public List<RepositoryResponse> searchRepositories(UUID userId, String query, int page, int perPage) {
        UserEntity user = userService.findById(userId);
        String accessToken = getAccessTokenFromUser(user);
        String scopedQuery = query + " user:" + user.getUsername();
        return repositoryMapper.toResponseList(gitHubResourceClient.searchRepositories(accessToken, scopedQuery, page, perPage).getItems());
    }

    @Override
    public List<PullRequestResponse> getPullRequests(UUID userId, String owner, String repo,
                                                     String state, int page, int perPage) {
        String accessToken = getGitHubAccessToken(userId);
        return pullRequestMapper.toResponseList(gitHubResourceClient.fetchPullRequests(accessToken, owner, repo, state, page, perPage));
    }

    @Override
    public PullRequestResponse getPullRequest(UUID userId, String owner, String repo, int pullNumber) {
        String accessToken = getGitHubAccessToken(userId);
        return pullRequestMapper.toResponse(gitHubResourceClient.fetchPullRequest(accessToken, owner, repo, pullNumber));
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
