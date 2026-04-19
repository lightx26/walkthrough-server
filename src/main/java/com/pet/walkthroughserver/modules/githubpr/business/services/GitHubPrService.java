package com.pet.walkthroughserver.modules.githubpr.business.services;

import java.util.List;
import java.util.UUID;

import com.pet.walkthroughserver.modules._shared.infra.github.dto.GitHubCommit;
import com.pet.walkthroughserver.modules._shared.infra.github.dto.GitHubPullRequest;
import com.pet.walkthroughserver.modules._shared.infra.github.dto.GitHubPullRequestFile;

public interface GitHubPrService {

    List<GitHubPullRequest> getPullRequests(UUID userId, String owner, String repo, String state, int page, int perPage);

    GitHubPullRequest getPullRequest(UUID userId, String owner, String repo, int pullNumber);

    List<GitHubCommit> getPullRequestCommits(UUID userId, String owner, String repo, int pullNumber);

    List<GitHubPullRequestFile> getPullRequestFiles(UUID userId, String owner, String repo, int pullNumber);

    List<GitHubPullRequestFile> getCommitFiles(UUID userId, String owner, String repo, String commitSha);

    List<GitHubPullRequest> getRecentPullRequests(UUID userId, int perPage);
}
