package com.pet.walkthroughserver.modules.walkthrough.business.events;

import java.util.UUID;

import com.pet.walkthroughserver.modules._shared.messaging.DomainEvent;

public sealed interface WalkthroughEvent extends DomainEvent
        permits WalkthroughCreatedEvent, WalkthroughUpdatedEvent, WalkthroughDeletedEvent {
    UUID walkthroughId();

    @Override
    default UUID aggregateId() {
        return walkthroughId();
    }
}
