package com.pet.walkthroughserver.modules._shared.infra.messaging.rabbit.listeners;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import com.pet.walkthroughserver.configs.RabbitMQConfig;
import com.pet.walkthroughserver.modules._shared.infra.messaging.ReviewSyncEventMessage;
import com.pet.walkthroughserver.modules.review.business.sync.ReviewSyncCommand;
import com.pet.walkthroughserver.modules.review.business.sync.ReviewSyncHandler;

import lombok.RequiredArgsConstructor;

/**
 * Thin RabbitMQ listener adapter — deserializes the transport message,
 * converts it to a domain command, and delegates to the business handler.
 */
@Component
@RequiredArgsConstructor
public class ReviewSyncRabbitListener {

    private final ReviewSyncHandler handler;

    @RabbitListener(queues = RabbitMQConfig.REVIEW_QUEUE)
    public void onMessage(ReviewSyncEventMessage message) {
        handler.handle(new ReviewSyncCommand(
                ReviewSyncCommand.Action.valueOf(message.getAction()),
                message.getReviewDecisionId(),
                message.getWalkthroughId(),
                message.getUserId(),
                message.getGithubReviewId(),
                message.getOwner(),
                message.getRepo(),
                message.getPrNumber()
        ));
    }
}
