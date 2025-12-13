package com.energy.monitoringservice.config;

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

    public static final String SYNC_EXCHANGE = "sync_fanout_exchange";
    public static final String MONITORING_SYNC_QUEUE = "monitoring_sync_queue";

    public static final String WEBSOCKET_EXCHANGE = "websocket_exchange";
    public static final String WEBSOCKET_ALERT_QUEUE = "websocket_alert_queue";
    public static final String WEBSOCKET_MEASUREMENT_QUEUE = "websocket_measurement_queue";

    @Value("${monitoring.replica.id:1}")
    private int replicaId;

    @Bean
    public String ingestQueueName() {
        return "ingest_queue_" + replicaId;
    }

    @Bean
    public Queue ingestQueue() {
        String queueName = ingestQueueName();
        return new Queue(queueName, true);
    }


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
    public Queue websocketAlertQueue() {
        return new Queue(WEBSOCKET_ALERT_QUEUE, true);
    }

    @Bean
    public Queue websocketMeasurementQueue() {
        return new Queue(WEBSOCKET_MEASUREMENT_QUEUE, true);
    }

    @Bean
    public DirectExchange websocketExchange() {
        return new DirectExchange(WEBSOCKET_EXCHANGE, true, false);
    }

    @Bean
    public Binding websocketAlertBinding() {
        return BindingBuilder.bind(websocketAlertQueue())
                .to(websocketExchange())
                .with("alert");
    }

    @Bean
    public Binding websocketMeasurementBinding() {
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