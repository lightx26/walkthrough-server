package com.pet.walkthroughserver.modules._shared.infra.github;

import com.pet.walkthroughserver.modules._shared.infra.github.dto.GitHubCommit;
import com.pet.walkthroughserver.modules._shared.infra.github.dto.GitHubPagedResult;
import com.pet.walkthroughserver.modules._shared.infra.github.dto.GitHubPullRequest;
import com.pet.walkthroughserver.modules._shared.infra.github.dto.GitHubPullRequestFile;
import com.pet.walkthroughserver.modules._shared.infra.github.dto.GitHubRepository;
import com.pet.walkthroughserver.modules._shared.infra.github.dto.GitHubSearchPrsResponse;
import com.pet.walkthroughserver.modules._shared.infra.github.dto.GitHubSearchReposResponse;

import java.util.List;

/**
 * GitHub client for resource-related operations.
 * Handles fetching repositories, pull requests, and search.
 */
public interface GitHubResourceClient {

    GitHubPagedResult<GitHubRepository> fetchUserRepositories(String accessToken, int page, int perPage, String sort);

    GitHubRepository fetchRepository(String accessToken, String owner, String repo);

    GitHubSearchReposResponse searchRepositories(String accessToken, String query, int page, int perPage);

    List<GitHubPullRequest> fetchPullRequests(String accessToken, String owner, String repo, String state, int page, int perPage);

    GitHubPullRequest fetchPullRequest(String accessToken, String owner, String repo, int pullNumber);

    List<GitHubCommit> fetchPullRequestCommits(String accessToken, String owner, String repo, int pullNumber);

    List<GitHubPullRequestFile> fetchPullRequestFiles(String accessToken, String owner, String repo, int pullNumber);

    List<GitHubPullRequestFile> fetchCommitFiles(String accessToken, String owner, String repo, String commitSha);

    Long createIssueComment(String accessToken, String owner, String repo, int issueNumber, String body);

    Long createPullReviewComment(String accessToken, String owner, String repo, int prNumber,
                                  String body, String commitId, String path, int position);

    /**
     * Submits a pull request review (approve / request changes / comment).
     *
     * @param event one of {@code APPROVE}, {@code REQUEST_CHANGES}, {@code COMMENT}
     * @return the GitHub review id
     */
    Long createPullReview(String accessToken, String owner, String repo, int prNumber,
                           String body, String event);

    /**
     * Dismisses a previously submitted pull request review.
     */
    void dismissPullReview(String accessToken, String owner, String repo, int prNumber,
                            long reviewId, String message);

    List<GitHubPullRequest> searchUserPullRequests(String accessToken, String username, int perPage);

    GitHubSearchPrsResponse searchPullRequests(String accessToken, String query, String username, int perPage);
}
