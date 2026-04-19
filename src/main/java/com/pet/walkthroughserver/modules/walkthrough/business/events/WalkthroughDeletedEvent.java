package com.pet.walkthroughserver.modules.walkthrough.business.events;

import java.time.Instant;
import java.util.UUID;

public record WalkthroughDeletedEvent(UUID walkthroughId, Instant occurredAt) implements WalkthroughEvent {
}
