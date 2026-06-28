package com.pet.walkthroughserver.modules.review.business.events;

import java.time.Instant;
import java.util.UUID;

import com.pet.walkthroughserver.modules._shared.messaging.DomainEvent;

/**
 * Emitted when a user withdraws their review decision. Carries the GitHub review
 * id and PR coordinates inline because the underlying row is already deleted by
 * the time the async handler runs.
 */
public record ReviewDecisionWithdrawnEvent(
        UUID walkthroughId,
        UUID userId,
        Long githubReviewId,
        String owner,
        String repo,
        Integer prNumber,
        Instant occurredAt
) implements DomainEvent {

    @Override
    public UUID aggregateId() {
        return walkthroughId;
    }

    @Override
    public String eventType() {
        return "REVIEW_DECISION_WITHDRAWN";
    }
}
