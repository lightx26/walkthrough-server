package com.pet.walkthroughserver.modules.github.service;

import com.pet.walkthroughserver.modules.github.dto.PullRequestResponse;
import com.pet.walkthroughserver.modules.github.dto.RepositoryResponse;

import java.util.List;
import java.util.UUID;

public interface GitHubService {

    List<RepositoryResponse> getUserRepositories(UUID userId, int page, int perPage, String sort);

    List<RepositoryResponse> searchRepositories(UUID userId, String query, int page, int perPage);

    List<PullRequestResponse> getPullRequests(UUID userId, String owner, String repo, String state, int page, int perPage);

    PullRequestResponse getPullRequest(UUID userId, String owner, String repo, int pullNumber);
}
