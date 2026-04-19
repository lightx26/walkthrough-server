package com.pet.walkthroughserver.modules.walkthrough.business.events;

import java.time.Instant;
import java.util.UUID;

public sealed interface WalkthroughEvent
        permits WalkthroughCreatedEvent, WalkthroughUpdatedEvent, WalkthroughDeletedEvent {
    UUID walkthroughId();
    Instant occurredAt();
}
