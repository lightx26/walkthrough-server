package com.pet.walkthroughserver.modules._shared.infra.messaging;

import java.util.UUID;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import com.pet.walkthroughserver.configs.RabbitMQConfig;
import com.pet.walkthroughserver.modules.walkthrough.business.events.WalkthroughCreatedEvent;
import com.pet.walkthroughserver.modules.walkthrough.business.events.WalkthroughDeletedEvent;
import com.pet.walkthroughserver.modules.walkthrough.business.events.WalkthroughEvent;
import com.pet.walkthroughserver.modules.walkthrough.business.events.WalkthroughEventPublisher;
import com.pet.walkthroughserver.modules.walkthrough.business.events.WalkthroughUpdatedEvent;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class WalkthroughEventPublisherAmqp implements WalkthroughEventPublisher {

    private final RabbitTemplate rabbitTemplate;

    @Override
    public void publish(WalkthroughEvent event) {
        String routingKey = resolveRoutingKey(event);
        String eventType = resolveEventType(event);

        WalkthroughEventMessage message = new WalkthroughEventMessage(
                UUID.randomUUID(),
                eventType,
                event.walkthroughId(),
                event.occurredAt().toString(),
                1
        );

        rabbitTemplate.convertAndSend(
                RabbitMQConfig.WALKTHROUGH_EVENTS_EXCHANGE,
                routingKey,
                message
        );

        log.info("Published {} for walkthrough {}", eventType, event.walkthroughId());
    }

    private String resolveRoutingKey(WalkthroughEvent event) {
        return switch (event) {
            case WalkthroughCreatedEvent e -> "walkthrough.created";
            case WalkthroughUpdatedEvent e -> "walkthrough.updated";
            case WalkthroughDeletedEvent e -> "walkthrough.deleted";
        };
    }

    private String resolveEventType(WalkthroughEvent event) {
        return switch (event) {
            case WalkthroughCreatedEvent e -> "WALKTHROUGH_CREATED";
            case WalkthroughUpdatedEvent e -> "WALKTHROUGH_UPDATED";
            case WalkthroughDeletedEvent e -> "WALKTHROUGH_DELETED";
        };
    }
}
