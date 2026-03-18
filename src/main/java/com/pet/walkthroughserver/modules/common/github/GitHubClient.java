package com.pet.walkthroughserver.modules.common.github;

import com.pet.walkthroughserver.modules.common.github.dto.GitHubAccessTokenResponse;
import com.pet.walkthroughserver.modules.common.github.dto.GitHubPullRequest;
import com.pet.walkthroughserver.modules.common.github.dto.GitHubRepository;
import com.pet.walkthroughserver.modules.common.github.dto.GitHubSearchReposResponse;
import com.pet.walkthroughserver.modules.common.github.dto.GitHubUserInfo;

import java.util.List;

public interface GitHubClient {

    GitHubAccessTokenResponse exchangeCodeForToken(String code);

    GitHubUserInfo fetchUserInfo(String accessToken);

    List<GitHubRepository> fetchUserRepositories(String accessToken, int page, int perPage, String sort);

    GitHubSearchReposResponse searchRepositories(String accessToken, String query, int page, int perPage);

    List<GitHubPullRequest> fetchPullRequests(String accessToken, String owner, String repo, String state, int page, int perPage);

    GitHubPullRequest fetchPullRequest(String accessToken, String owner, String repo, int pullNumber);
}
