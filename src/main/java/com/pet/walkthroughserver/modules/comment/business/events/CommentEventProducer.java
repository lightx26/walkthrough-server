package com.pet.walkthroughserver.modules.comment.business.events;

import com.pet.walkthroughserver.configs.RabbitMQConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class CommentEventProducer {

    private final RabbitTemplate rabbitTemplate;

    public void publish(CommentCreatedEvent event) {
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.COMMENT_EXCHANGE,
                RabbitMQConfig.COMMENT_ROUTING_KEY,
                event
        );
        log.info("Published CommentCreatedEvent for comment {}", event.getCommentId());
    }
}
