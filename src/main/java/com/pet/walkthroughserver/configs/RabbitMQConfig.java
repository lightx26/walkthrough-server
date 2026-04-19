package com.pet.walkthroughserver.configs;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    // ── Comment events ──
    public static final String COMMENT_QUEUE = "walkthrough.comment.sync";
    public static final String COMMENT_EXCHANGE = "walkthrough.comment";
    public static final String COMMENT_ROUTING_KEY = "comment.created";

    // ── Walkthrough search events ──
    public static final String WALKTHROUGH_EVENTS_EXCHANGE = "walkthrough.events";
    public static final String WALKTHROUGH_SEARCH_SYNC_QUEUE = "walkthrough.search.sync.q";
    public static final String WALKTHROUGH_SEARCH_SYNC_DLX = "walkthrough.search.sync.dlx";
    public static final String WALKTHROUGH_SEARCH_SYNC_DLQ = "walkthrough.search.sync.dlq";
    public static final String WALKTHROUGH_EVENTS_ROUTING_KEY = "walkthrough.*";

    @Bean
    Queue commentQueue() {
        return new Queue(COMMENT_QUEUE, true);
    }

    @Bean
    DirectExchange commentExchange() {
        return new DirectExchange(COMMENT_EXCHANGE);
    }

    @Bean
    Binding commentBinding(Queue commentQueue, DirectExchange commentExchange) {
        return BindingBuilder.bind(commentQueue).to(commentExchange).with(COMMENT_ROUTING_KEY);
    }

    // ── Walkthrough search topology ──

    @Bean
    TopicExchange walkthroughEventsExchange() {
        return new TopicExchange(WALKTHROUGH_EVENTS_EXCHANGE);
    }

    @Bean
    DirectExchange walkthroughSearchDlx() {
        return new DirectExchange(WALKTHROUGH_SEARCH_SYNC_DLX);
    }

    @Bean
    Queue walkthroughSearchDlq() {
        return QueueBuilder.durable(WALKTHROUGH_SEARCH_SYNC_DLQ).build();
    }

    @Bean
    Binding walkthroughSearchDlqBinding(Queue walkthroughSearchDlq, DirectExchange walkthroughSearchDlx) {
        return BindingBuilder.bind(walkthroughSearchDlq).to(walkthroughSearchDlx).with(WALKTHROUGH_SEARCH_SYNC_DLQ);
    }

    @Bean
    Queue walkthroughSearchSyncQueue() {
        return QueueBuilder.durable(WALKTHROUGH_SEARCH_SYNC_QUEUE)
                .withArgument("x-dead-letter-exchange", WALKTHROUGH_SEARCH_SYNC_DLX)
                .withArgument("x-dead-letter-routing-key", WALKTHROUGH_SEARCH_SYNC_DLQ)
                .build();
    }

    @Bean
    Binding walkthroughSearchSyncBinding(Queue walkthroughSearchSyncQueue, TopicExchange walkthroughEventsExchange) {
        return BindingBuilder.bind(walkthroughSearchSyncQueue).to(walkthroughEventsExchange).with(WALKTHROUGH_EVENTS_ROUTING_KEY);
    }

    @Bean
    JacksonJsonMessageConverter jsonMessageConverter() {
        return new JacksonJsonMessageConverter();
    }

    @Bean
    RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, JacksonJsonMessageConverter converter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(converter);
        return template;
    }
}
