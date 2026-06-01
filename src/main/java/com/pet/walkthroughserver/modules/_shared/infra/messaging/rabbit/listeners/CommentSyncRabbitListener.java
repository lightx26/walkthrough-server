package com.pet.walkthroughserver.modules._shared.infra.messaging.rabbit.listeners;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import com.pet.walkthroughserver.configs.RabbitMQConfig;
import com.pet.walkthroughserver.modules._shared.infra.messaging.CommentEventMessage;
import com.pet.walkthroughserver.modules.comment.business.sync.CommentSyncCommand;
import com.pet.walkthroughserver.modules.comment.business.sync.CommentSyncHandler;

import lombok.RequiredArgsConstructor;

/**
 * Thin RabbitMQ listener adapter — deserializes the transport message,
 * converts it to a domain command, and delegates to the business handler.
 */
@Component
@RequiredArgsConstructor
public class CommentSyncRabbitListener {

    private final CommentSyncHandler handler;

    @RabbitListener(queues = RabbitMQConfig.COMMENT_QUEUE)
    public void onMessage(CommentEventMessage message) {
        handler.handle(new CommentSyncCommand(
                message.getCommentId(),
                message.getWalkthroughId(),
                message.getUserId(),
                message.getContent(),
                message.getWalkthroughFileId(),
                message.getDiffPosition()
        ));
    }
}
