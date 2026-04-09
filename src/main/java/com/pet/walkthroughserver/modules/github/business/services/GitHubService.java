package com.pet.walkthroughserver.modules.github.business.services;

import com.pet.walkthroughserver.modules._shared.infra.github.dto.GitHubCommit;
import com.pet.walkthroughserver.modules._shared.infra.github.dto.GitHubPullRequest;
import com.pet.walkthroughserver.modules._shared.infra.github.dto.GitHubPullRequestFile;
import com.pet.walkthroughserver.modules._shared.infra.github.dto.GitHubRepository;

import java.util.List;
import java.util.UUID;

public interface GitHubService {

    List<GitHubRepository> getUserRepositories(UUID userId, int page, int perPage, String sort);

    List<GitHubRepository> searchRepositories(UUID userId, String query, int page, int perPage);

    List<GitHubPullRequest> getPullRequests(UUID userId, String owner, String repo, String state, int page, int perPage);

    GitHubPullRequest getPullRequest(UUID userId, String owner, String repo, int pullNumber);

    List<GitHubCommit> getPullRequestCommits(UUID userId, String owner, String repo, int pullNumber);

    List<GitHubPullRequestFile> getPullRequestFiles(UUID userId, String owner, String repo, int pullNumber);

    List<GitHubPullRequestFile> getCommitFiles(UUID userId, String owner, String repo, String commitSha);

    Long createPrComment(UUID userId, String owner, String repo, int prNumber, String body);

    Long createPrReviewComment(UUID userId, String owner, String repo, int prNumber,
                                String body, String commitId, String path, int position);

    List<GitHubPullRequest> getRecentPullRequests(UUID userId, int perPage);
}
