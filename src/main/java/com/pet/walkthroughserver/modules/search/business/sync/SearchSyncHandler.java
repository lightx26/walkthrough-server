package com.pet.walkthroughserver.modules.search.business.sync;

/**
 * Port: handles inbound search-sync commands triggered by walkthrough events.
 * No messaging imports — the infra listener adapter converts
 * transport messages into {@link SearchSyncCommand} and calls this.
 */
public interface SearchSyncHandler {

    void handle(SearchSyncCommand command);
}
