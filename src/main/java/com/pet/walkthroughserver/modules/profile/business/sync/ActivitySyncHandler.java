package com.pet.walkthroughserver.modules.profile.business.sync;

/**
 * Port: handles inbound activity-sync commands triggered by walkthrough events.
 * No messaging imports — the infra listener adapter converts
 * transport messages into {@link ActivitySyncCommand} and calls this.
 */
public interface ActivitySyncHandler {

    void handle(ActivitySyncCommand command);
}
