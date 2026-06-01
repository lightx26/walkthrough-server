package com.pet.walkthroughserver.modules._shared.infra.messaging.rabbit;

/**
 * Immutable routing destination for a single event type.
 */
public record EventRoute(String exchange, String routingKey) {
}
