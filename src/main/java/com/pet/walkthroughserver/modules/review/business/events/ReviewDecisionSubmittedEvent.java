package com.pet.walkthroughserver.modules.review.business.events;

import java.time.Instant;
import java.util.UUID;

import com.pet.walkthroughserver.modules._shared.messaging.DomainEvent;

public record ReviewDecisionSubmittedEvent(
        UUID reviewDecisionId,
        UUID walkthroughId,
        UUID userId,
        Instant occurredAt
) implements DomainEvent {

    @Override
    public UUID aggregateId() {
        return reviewDecisionId;
    }

    @Override
    public String eventType() {
        return "REVIEW_DECISION_SUBMITTED";
    }
}
