package com.pet.walkthroughserver.configs;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String COMMENT_QUEUE = "walkthrough.comment.sync";
    public static final String COMMENT_EXCHANGE = "walkthrough.comment";
    public static final String COMMENT_ROUTING_KEY = "comment.created";

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
