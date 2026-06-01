package com.pet.walkthroughserver.modules.comment.business.events;

import java.time.Instant;
import java.util.UUID;

import com.pet.walkthroughserver.modules._shared.messaging.DomainEvent;

public record CommentCreatedEvent(
        UUID commentId,
        UUID walkthroughId,
        UUID userId,
        String content,
        UUID walkthroughFileId,
        Integer diffPosition,
        Instant occurredAt
) implements DomainEvent {

    @Override
    public UUID aggregateId() {
        return commentId;
    }

    @Override
    public String eventType() {
        return "COMMENT_CREATED";
    }
}
