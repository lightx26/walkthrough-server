package com.pet.walkthroughserver.modules.comment.business.services;

import java.util.UUID;

import org.springframework.stereotype.Service;

import com.pet.walkthroughserver.modules._shared.infra.github.GitHubResourceClient;
import com.pet.walkthroughserver.modules._shared.infra.github.GitHubTokenProvider;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class GitHubCommentServiceImpl implements GitHubCommentService {

    private final GitHubResourceClient gitHubResourceClient;
    private final GitHubTokenProvider gitHubTokenProvider;

    @Override
    public Long createPrComment(UUID userId, String owner, String repo, int prNumber, String body) {
        String accessToken = gitHubTokenProvider.accessTokenFor(userId);
        return gitHubResourceClient.createIssueComment(accessToken, owner, repo, prNumber, body);
    }

    @Override
    public Long createPrReviewComment(UUID userId, String owner, String repo, int prNumber,
                                       String body, String commitId, String path, int position) {
        String accessToken = gitHubTokenProvider.accessTokenFor(userId);
        return gitHubResourceClient.createPullReviewComment(accessToken, owner, repo, prNumber, body, commitId, path, position);
    }
}
