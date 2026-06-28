package com.pet.walkthroughserver.modules.review.business.sync;

/**
 * Port: handles inbound review-decision-sync commands.
 * No messaging imports — the infra listener adapter converts
 * transport messages into {@link ReviewSyncCommand} and calls this.
 */
public interface ReviewSyncHandler {

    void handle(ReviewSyncCommand command);
}
