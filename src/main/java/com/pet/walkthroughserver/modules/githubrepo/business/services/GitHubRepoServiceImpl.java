package com.pet.walkthroughserver.modules.githubrepo.business.services;

import com.pet.walkthroughserver.modules._shared.infra.github.GitHubResourceClient;
import com.pet.walkthroughserver.modules._shared.infra.github.dto.GitHubRepository;
import com.pet.walkthroughserver.modules._shared.infra.github.exceptions.GitHubAccessTokenNotFoundException;
import com.pet.walkthroughserver.modules.user.business.services.UserService;
import com.pet.walkthroughserver.modules.user.repository.UserEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GitHubRepoServiceImpl implements GitHubRepoService {

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
    public GitHubRepository getRepository(UUID userId, String owner, String repo) {
        String accessToken = getGitHubAccessToken(userId);
        return gitHubResourceClient.fetchRepository(accessToken, owner, repo);
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
