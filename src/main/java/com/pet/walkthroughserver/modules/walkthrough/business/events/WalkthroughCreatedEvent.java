package com.pet.walkthroughserver.modules.walkthrough.business.events;

import java.time.Instant;
import java.util.UUID;

public record WalkthroughCreatedEvent(UUID walkthroughId, Instant occurredAt) implements WalkthroughEvent {

    @Override
    public String eventType() {
        return "WALKTHROUGH_CREATED";
    }
}
