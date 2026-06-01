package com.pet.walkthroughserver.modules._shared.messaging;

import java.time.Instant;
import java.util.UUID;

/**
 * Port: every domain event in the system implements this contract.
 * No infrastructure dependencies — business code publishes these;
 * adapters (Rabbit, Kafka, …) decide how to serialize and route them.
 */
public interface DomainEvent {

    UUID aggregateId();

    Instant occurredAt();

    String eventType();
}
