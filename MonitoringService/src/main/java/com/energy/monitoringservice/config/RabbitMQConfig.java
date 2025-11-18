package com.energy.monitoringservice.config;

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

    public static final String SYNC_EXCHANGE = "sync_fanout_exchange";

    public static final String MONITORING_SYNC_QUEUE = "monitoring_sync_queue";

    public static final String SENSOR_DATA_QUEUE = "device_measurements";
    public static final String SENSOR_EXCHANGE = "sensor_exchange";

    @Bean
    public Queue monitoringSyncQueue() {
        return new Queue(MONITORING_SYNC_QUEUE, true);
    }

    @Bean
    public FanoutExchange syncFanoutExchange() {
        return new FanoutExchange(SYNC_EXCHANGE, true, false);
    }

    @Bean
    public Binding monitoringSyncBinding() {
        return BindingBuilder.bind(monitoringSyncQueue())
                .to(syncFanoutExchange());
    }

    @Bean
    public Queue sensorDataQueue() {
        return new Queue(SENSOR_DATA_QUEUE, true);
    }

    @Bean
    public DirectExchange sensorExchange() {
        return new DirectExchange(SENSOR_EXCHANGE, true, false);
    }

    @Bean
    public Binding sensorBinding() {
        return BindingBuilder.bind(sensorDataQueue())
                .to(sensorExchange())
                .with("sensor.data");
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