package com.pet.walkthroughserver.modules._shared.messaging;

/**
 * Port: outbound event publishing.
 * Business code depends only on this interface — the concrete transport
 * (RabbitMQ, Kafka, in-memory, …) is chosen by the adapter layer.
 */
public interface DomainEventPublisher {

    void publish(DomainEvent event);
}
