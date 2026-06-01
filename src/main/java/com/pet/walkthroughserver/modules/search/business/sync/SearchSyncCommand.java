package com.pet.walkthroughserver.modules.search.business.sync;

import java.util.UUID;

/**
 * Inbound command for syncing a walkthrough to the search index.
 * Transport-agnostic — built from whatever message format the listener adapter receives.
 */
public record SearchSyncCommand(
        String eventType,
        UUID walkthroughId
) {
}
