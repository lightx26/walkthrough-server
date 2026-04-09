package com.pet.walkthroughserver.modules._shared.infra.github;

import com.pet.walkthroughserver.modules._shared.infra.github.dto.GitHubCommit;
import com.pet.walkthroughserver.modules._shared.infra.github.dto.GitHubPullRequest;
import com.pet.walkthroughserver.modules._shared.infra.github.dto.GitHubPullRequestFile;
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

    List<GitHubCommit> fetchPullRequestCommits(String accessToken, String owner, String repo, int pullNumber);

    List<GitHubPullRequestFile> fetchPullRequestFiles(String accessToken, String owner, String repo, int pullNumber);

    List<GitHubPullRequestFile> fetchCommitFiles(String accessToken, String owner, String repo, String commitSha);

    Long createIssueComment(String accessToken, String owner, String repo, int issueNumber, String body);

    Long createPullReviewComment(String accessToken, String owner, String repo, int prNumber,
                                  String body, String commitId, String path, int position);

    List<GitHubPullRequest> searchUserPullRequests(String accessToken, String username, int perPage);
}
