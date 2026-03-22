package com.pet.walkthroughserver.modules._shared.infra.github;

import com.pet.walkthroughserver.modules._shared.infra.github.dto.GitHubPullRequest;
import com.pet.walkthroughserver.modules._shared.infra.github.dto.GitHubRepository;
import com.pet.walkthroughserver.modules._shared.infra.github.dto.GitHubSearchReposResponse;

import java.util.List;

/**
 * GitHub client for resource-related operations.
 * Handles fetching repositories, pull requests, and search.
 */
public interface GitHubResourceClient {

    List<GitHubRepository> fetchUserRepositories(String accessToken, int page, int perPage, String sort);

    GitHubSearchReposResponse searchRepositories(String accessToken, String query, int page, int perPage);

    List<GitHubPullRequest> fetchPullRequests(String accessToken, String owner, String repo, String state, int page, int perPage);

    GitHubPullRequest fetchPullRequest(String accessToken, String owner, String repo, int pullNumber);
}
