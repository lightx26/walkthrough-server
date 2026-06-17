package com.pet.walkthroughserver.modules._shared.infra.messaging.rabbit;

import java.util.UUID;

import org.springframework.stereotype.Component;

import com.pet.walkthroughserver.modules._shared.infra.messaging.CommentEventMessage;
import com.pet.walkthroughserver.modules._shared.infra.messaging.RiskScanEventMessage;
import com.pet.walkthroughserver.modules._shared.infra.messaging.WalkthroughEventMessage;
import com.pet.walkthroughserver.modules._shared.messaging.DomainEvent;
import com.pet.walkthroughserver.modules.comment.business.events.CommentCreatedEvent;
import com.pet.walkthroughserver.modules.riskzone.business.sync.RiskScanRequestedEvent;
import com.pet.walkthroughserver.modules.walkthrough.business.events.WalkthroughEvent;

/**
 * Converts domain events into transport DTOs suitable for the message broker.
 */
@Component
public class EventMessageFactory {

    public Object toMessage(DomainEvent event) {
        return switch (event) {
            case WalkthroughEvent e -> toWalkthroughMessage(e);
            case CommentCreatedEvent e -> toCommentMessage(e);
            case RiskScanRequestedEvent e -> toRiskScanMessage(e);
            default -> throw new IllegalArgumentException(
                    "No message mapping for event type: " + event.eventType());
        };
    }

    private WalkthroughEventMessage toWalkthroughMessage(WalkthroughEvent event) {
        return new WalkthroughEventMessage(
                UUID.randomUUID(),
                event.eventType(),
                event.walkthroughId(),
                event.occurredAt().toString(),
                1
        );
    }

    private CommentEventMessage toCommentMessage(CommentCreatedEvent event) {
        return new CommentEventMessage(
                event.commentId(),
                event.walkthroughId(),
                event.userId(),
                event.content(),
                event.walkthroughFileId(),
                event.diffPosition()
        );
    }

    private RiskScanEventMessage toRiskScanMessage(RiskScanRequestedEvent event) {
        return new RiskScanEventMessage(
                event.scanId(),
                event.walkthroughId(),
                event.occurredAt().toString()
        );
    }
}
