package com.pet.walkthroughserver.modules._shared.infra.messaging.rabbit;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import com.pet.walkthroughserver.modules._shared.messaging.DomainEvent;
import com.pet.walkthroughserver.modules._shared.messaging.DomainEventPublisher;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Single RabbitMQ adapter that publishes all domain events.
 * Replaces per-module publishers (WalkthroughEventPublisherAmqp, CommentEventProducer).
 *
 * To switch to Kafka: implement a KafkaDomainEventPublisher with
 * {@code @ConditionalOnProperty(messaging.transport=kafka)} and disable this bean.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RabbitDomainEventPublisher implements DomainEventPublisher {

    private final RabbitTemplate rabbitTemplate;
    private final EventRoutingRegistry routes;
    private final EventMessageFactory messages;

    @Override
    public void publish(DomainEvent event) {
        EventRoute route = routes.routeFor(event.eventType());
        Object message = messages.toMessage(event);

        rabbitTemplate.convertAndSend(route.exchange(), route.routingKey(), message);

        log.info("Published {} for aggregate {}", event.eventType(), event.aggregateId());
    }
}
