package com.pet.walkthroughserver.modules.profile.business.sync;

import java.time.Instant;
import java.util.UUID;

/**
 * Inbound command for recording a walkthrough activity entry.
 * Transport-agnostic — built from whatever message format the listener adapter receives.
 */
public record ActivitySyncCommand(
        String eventType,
        UUID walkthroughId,
        Instant occurredAt
) {
}
