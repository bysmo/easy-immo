package com.easyimmo.notification.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    @Bean
    public Jackson2JsonMessageConverter producerJackson2MessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public Queue agencyCreatedQueue() {
        return new Queue("agency.created.queue", true);
    }

    @Bean
    public TopicExchange easyImmoExchange() {
        return new TopicExchange("easy-immo.exchange", true, false);
    }

    @Bean
    public Binding agencyCreatedBinding() {
        return BindingBuilder.bind(agencyCreatedQueue())
                .to(easyImmoExchange())
                .with("agency.created");
    }
}
