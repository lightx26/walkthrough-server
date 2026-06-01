package com.pet.walkthroughserver.modules._shared.infra.messaging.rabbit.listeners;

import java.time.Instant;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import com.pet.walkthroughserver.configs.RabbitMQConfig;
import com.pet.walkthroughserver.modules._shared.infra.messaging.WalkthroughEventMessage;
import com.pet.walkthroughserver.modules.profile.business.sync.ActivitySyncCommand;
import com.pet.walkthroughserver.modules.profile.business.sync.ActivitySyncHandler;

import lombok.RequiredArgsConstructor;

/**
 * Thin RabbitMQ listener adapter for activity sync.
 */
@Component
@RequiredArgsConstructor
public class ActivitySyncRabbitListener {

    private final ActivitySyncHandler handler;

    @RabbitListener(queues = RabbitMQConfig.ACTIVITY_SYNC_QUEUE)
    public void onMessage(WalkthroughEventMessage message) {
        handler.handle(new ActivitySyncCommand(
                message.getEventType(),
                message.getWalkthroughId(),
                Instant.parse(message.getOccurredAt())
        ));
    }
}
