package com.pet.walkthroughserver.modules.review.repository;

/**
 * Lifecycle of a review decision's synchronization to GitHub.
 */
public enum ReviewSyncStatus {

    PENDING,
    SYNCED,
    FAILED;
}
