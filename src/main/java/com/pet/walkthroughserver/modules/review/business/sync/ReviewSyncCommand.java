package com.pet.walkthroughserver.modules.review.business.sync;

import java.util.UUID;

/**
 * Inbound command for syncing a review decision to GitHub.
 * Transport-agnostic — built from whatever message format the listener adapter receives.
 */
public record ReviewSyncCommand(
        Action action,
        UUID reviewDecisionId,
        UUID walkthroughId,
        UUID userId,
        Long githubReviewId,
        String owner,
        String repo,
        Integer prNumber
) {

    public enum Action {
        SUBMIT,
        DISMISS
    }
}
