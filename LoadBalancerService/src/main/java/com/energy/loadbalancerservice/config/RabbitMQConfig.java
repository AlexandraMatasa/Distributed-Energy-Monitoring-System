package com.energy.loadbalancerservice.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableRabbit
public class RabbitMQConfig {

    public static final String CENTRAL_QUEUE = "device_measurements";
    public static final String SENSOR_EXCHANGE = "sensor_exchange";

    public static final String INGEST_QUEUE_PREFIX = "ingest_queue_";

    @Value("${loadbalancer.replica.count:3}")
    private int replicaCount;

    @Bean
    public Queue centralQueue() {
        return new Queue(CENTRAL_QUEUE, true);
    }

    @Bean
    public DirectExchange sensorExchange() {
        return new DirectExchange(SENSOR_EXCHANGE, true, false);
    }

    @Bean
    public Binding centralBinding() {
        return BindingBuilder.bind(centralQueue())
                .to(sensorExchange())
                .with("sensor.data");
    }

    @Bean
    public Queue ingestQueue1() {
        return new Queue(INGEST_QUEUE_PREFIX + "1", true);
    }

    @Bean
    public Queue ingestQueue2() {
        return new Queue(INGEST_QUEUE_PREFIX + "2", true);
    }

    @Bean
    public Queue ingestQueue3() {
        return new Queue(INGEST_QUEUE_PREFIX + "3", true);
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
