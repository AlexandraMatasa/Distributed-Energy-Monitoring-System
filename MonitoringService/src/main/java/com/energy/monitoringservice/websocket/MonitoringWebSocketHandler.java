package com.energy.monitoringservice.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class MonitoringWebSocketHandler extends TextWebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(MonitoringWebSocketHandler.class);

    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();
    private final Map<String, UUID> sessionDeviceMap = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper;

    public MonitoringWebSocketHandler() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String sessionId = session.getId();
        sessions.put(sessionId, session);
        log.info("WebSocket connection established: {}", sessionId);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String payload = message.getPayload();
        log.info("Received message from {}: {}", session.getId(), payload);

        try {
            Map<String, String> data = objectMapper.readValue(payload, Map.class);
            String action = data.get("action");

            if ("subscribe".equals(action)) {
                String deviceIdStr = data.get("deviceId");
                UUID deviceId = UUID.fromString(deviceIdStr);
                sessionDeviceMap.put(session.getId(), deviceId);
                log.info("Session {} subscribed to device {}", session.getId(), deviceId);

                Map<String, String> response = Map.of(
                        "type", "subscribed",
                        "deviceId", deviceIdStr,
                        "message", "Successfully subscribed to device updates"
                );
                session.sendMessage(new TextMessage(objectMapper.writeValueAsString(response)));
            }
        } catch (Exception e) {
            log.error("Error handling message: {}", e.getMessage(), e);
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        String sessionId = session.getId();
        sessions.remove(sessionId);
        UUID deviceId = sessionDeviceMap.remove(sessionId);
        log.info("WebSocket connection closed: {} (was subscribed to device: {})", sessionId, deviceId);
    }

    public void broadcastNewMeasurement(UUID deviceId, Object measurementData) {
        log.info("Starting broadcast for device: {}", deviceId);
        log.info("Active sessions: {}", sessions.size());
        log.info("Device subscriptions: {}", sessionDeviceMap.size());

        sessionDeviceMap.forEach((sessionId, subscribedDeviceId) -> {
            log.info("Checking session {} subscribed to {}", sessionId, subscribedDeviceId);

            if (subscribedDeviceId.equals(deviceId)) {
                WebSocketSession session = sessions.get(sessionId);
                log.info("Found matching session {} for device {}", sessionId, deviceId);

                if (session != null && session.isOpen()) {
                    try {
                        Map<String, Object> message = Map.of(
                                "type", "newMeasurement",
                                "deviceId", deviceId.toString(),
                                "data", measurementData
                        );
                        String json = objectMapper.writeValueAsString(message);
                        session.sendMessage(new TextMessage(json));
                        log.info("Sent measurement to session {}: {}", sessionId, json);
                    } catch (IOException e) {
                        log.error("Error sending message to session {}: {}", sessionId, e.getMessage());
                    }
                } else {
                    log.warn("Session {} is null or closed", sessionId);
                }
            } else {
                log.info("Skipping session {} (subscribed to {})", sessionId, subscribedDeviceId);
            }
        });

        log.info("Broadcast complete for device: {}", deviceId);
    }
}