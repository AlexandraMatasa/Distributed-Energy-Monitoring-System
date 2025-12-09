package com.energy.communicationservice.service;

import com.energy.communicationservice.config.RabbitMQConfig;
import com.energy.communicationservice.dto.WebSocketMessageDTO;
import com.energy.communicationservice.handler.NotificationWebSocketHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class WebSocketConsumerService {

    private static final Logger log = LoggerFactory.getLogger(WebSocketConsumerService.class);

    private final NotificationWebSocketHandler webSocketHandler;

    @Autowired
    public WebSocketConsumerService(NotificationWebSocketHandler webSocketHandler) {
        this.webSocketHandler = webSocketHandler;
    }

    @RabbitListener(queues = RabbitMQConfig.WEBSOCKET_ALERT_QUEUE)
    public void handleAlert(WebSocketMessageDTO message) {
        log.info("Received ALERT from RabbitMQ: userId={}, deviceId={}",
                message.getUserId(), message.getDeviceId());

        if (message.getUserId() != null) {
            webSocketHandler.broadcastAlertToUser(message.getUserId(), message.getData());
        } else {
            log.warn("Alert without userId, cannot send to specific user");
        }
    }

    @RabbitListener(queues = RabbitMQConfig.WEBSOCKET_MEASUREMENT_QUEUE)
    public void handleMeasurementUpdate(WebSocketMessageDTO message) {
        log.info("Received MEASUREMENT update from RabbitMQ: deviceId={}", message.getDeviceId());

        if (message.getDeviceId() != null) {
            webSocketHandler.broadcastNewMeasurement(message.getDeviceId(), message.getData());
        } else {
            log.warn("Measurement update without deviceId");
        }
    }
}
