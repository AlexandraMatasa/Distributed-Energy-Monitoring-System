package com.energy.communicationservice.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableRabbit
public class RabbitMQConfig {

    public static final String WEBSOCKET_ALERT_QUEUE = "websocket_alert_queue";
    public static final String WEBSOCKET_MEASUREMENT_QUEUE = "websocket_measurement_queue";

    public static final String WEBSOCKET_EXCHANGE = "websocket_exchange";

    public static final String CHAT_QUEUE = "chat_queue";

    @Bean
    public Queue websocketAlertQueue() {
        return new Queue(WEBSOCKET_ALERT_QUEUE, true);
    }

    @Bean
    public Queue websocketMeasurementQueue() {
        return new Queue(WEBSOCKET_MEASUREMENT_QUEUE, true);
    }

    @Bean
    public Queue chatQueue() {
        return new Queue(CHAT_QUEUE, true);
    }

    @Bean
    public DirectExchange websocketExchange() {
        return new DirectExchange(WEBSOCKET_EXCHANGE, true, false);
    }

    @Bean
    public Binding alertBinding() {
        return BindingBuilder.bind(websocketAlertQueue())
                .to(websocketExchange())
                .with("alert");
    }

    @Bean
    public Binding measurementBinding() {
        return BindingBuilder.bind(websocketMeasurementQueue())
                .to(websocketExchange())
                .with("measurement");
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jsonMessageConverter());
        return template;
    }
}