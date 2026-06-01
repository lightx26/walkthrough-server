package com.pet.walkthroughserver.modules._shared.infra.messaging.rabbit.listeners;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import com.pet.walkthroughserver.configs.RabbitMQConfig;
import com.pet.walkthroughserver.modules._shared.infra.messaging.WalkthroughEventMessage;
import com.pet.walkthroughserver.modules.search.business.sync.SearchSyncCommand;
import com.pet.walkthroughserver.modules.search.business.sync.SearchSyncHandler;

import lombok.RequiredArgsConstructor;

/**
 * Thin RabbitMQ listener adapter for search index sync.
 */
@Component
@RequiredArgsConstructor
public class SearchSyncRabbitListener {

    private final SearchSyncHandler handler;

    @RabbitListener(queues = RabbitMQConfig.WALKTHROUGH_SEARCH_SYNC_QUEUE)
    public void onMessage(WalkthroughEventMessage message) {
        handler.handle(new SearchSyncCommand(
                message.getEventType(),
                message.getWalkthroughId()
        ));
    }
}
