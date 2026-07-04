package com.easyimmo.tenant.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String EXCHANGE = "easy-immo.exchange";

    /**
     * Convertisseur JSON pour RabbitMQ — remplace le SimpleMessageConverter
     * qui ne supporte que String/byte[]/Serializable.
     */
    @Bean
    public MessageConverter jackson2JsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    /**
     * Configure le RabbitTemplate global avec le convertisseur JSON.
     */
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jackson2JsonMessageConverter());
        return template;
    }

    /**
     * Exchange topic principal de l'application.
     */
    @Bean
    public TopicExchange easyImmoExchange() {
        return ExchangeBuilder.topicExchange(EXCHANGE).durable(true).build();
    }
}
