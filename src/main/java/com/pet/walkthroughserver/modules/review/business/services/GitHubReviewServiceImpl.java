package com.pet.walkthroughserver.modules.review.business.services;

import java.util.UUID;

import org.springframework.stereotype.Service;

import com.pet.walkthroughserver.modules._shared.infra.github.GitHubResourceClient;
import com.pet.walkthroughserver.modules._shared.infra.github.GitHubTokenProvider;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class GitHubReviewServiceImpl implements GitHubReviewService {

    private final GitHubResourceClient gitHubResourceClient;
    private final GitHubTokenProvider gitHubTokenProvider;

    @Override
    public Long submitReview(UUID userId, String owner, String repo, int prNumber, String body, String event) {
        String accessToken = gitHubTokenProvider.accessTokenFor(userId);
        return gitHubResourceClient.createPullReview(accessToken, owner, repo, prNumber, body, event);
    }

    @Override
    public void dismissReview(UUID userId, String owner, String repo, int prNumber, long reviewId, String message) {
        String accessToken = gitHubTokenProvider.accessTokenFor(userId);
        gitHubResourceClient.dismissPullReview(accessToken, owner, repo, prNumber, reviewId, message);
    }
}
