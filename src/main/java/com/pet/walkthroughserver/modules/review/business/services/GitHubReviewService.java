package com.pet.walkthroughserver.modules.review.business.services;

import java.util.UUID;

public interface GitHubReviewService {

    /**
     * Submits a pull request review on GitHub.
     *
     * @param event one of {@code APPROVE}, {@code REQUEST_CHANGES}, {@code COMMENT}
     * @return the GitHub review id
     */
    Long submitReview(UUID userId, String owner, String repo, int prNumber, String body, String event);

    /**
     * Dismisses a previously submitted pull request review on GitHub.
     */
    void dismissReview(UUID userId, String owner, String repo, int prNumber, long reviewId, String message);
}
