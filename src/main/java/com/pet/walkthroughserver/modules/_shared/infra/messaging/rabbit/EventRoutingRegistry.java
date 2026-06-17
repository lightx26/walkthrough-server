package com.pet.walkthroughserver.modules._shared.infra.messaging.rabbit;

import java.util.Map;

import org.springframework.stereotype.Component;

import com.pet.walkthroughserver.configs.RabbitMQConfig;

/**
 * Declarative mapping: event type → (exchange, routingKey).
 * To add a new event, add one entry here — zero changes in the publisher.
 */
@Component
public class EventRoutingRegistry {

    private static final Map<String, EventRoute> ROUTES = Map.of(
            "WALKTHROUGH_CREATED",  new EventRoute(RabbitMQConfig.WALKTHROUGH_EVENTS_EXCHANGE, "walkthrough.created"),
            "WALKTHROUGH_UPDATED",  new EventRoute(RabbitMQConfig.WALKTHROUGH_EVENTS_EXCHANGE, "walkthrough.updated"),
            "WALKTHROUGH_DELETED",  new EventRoute(RabbitMQConfig.WALKTHROUGH_EVENTS_EXCHANGE, "walkthrough.deleted"),
            "COMMENT_CREATED",      new EventRoute(RabbitMQConfig.COMMENT_EXCHANGE, RabbitMQConfig.COMMENT_ROUTING_KEY),
            "RISK_SCAN_REQUESTED",  new EventRoute(RabbitMQConfig.WALKTHROUGH_EVENTS_EXCHANGE, RabbitMQConfig.RISK_SCAN_ROUTING_KEY)
    );

    public EventRoute routeFor(String eventType) {
        EventRoute route = ROUTES.get(eventType);
        if (route == null) {
            throw new IllegalArgumentException("No route registered for event type: " + eventType);
        }
        return route;
    }
}
