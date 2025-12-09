package com.energy.communicationservice.handler;

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
public class NotificationWebSocketHandler extends TextWebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(NotificationWebSocketHandler.class);

    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();
    private final Map<String, UUID> sessionDeviceMap = new ConcurrentHashMap<>();
    private final Map<String, UUID> sessionUserMap = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper;

    public NotificationWebSocketHandler() {
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
                String userIdStr = data.get("userId");

                UUID deviceId = UUID.fromString(deviceIdStr);
                UUID userId = UUID.fromString(userIdStr);

                sessionDeviceMap.put(session.getId(), deviceId);
                sessionUserMap.put(session.getId(), userId);

                log.info("Session {} subscribed to device {} for user {}", session.getId(), deviceId, userId);

                Map<String, String> response = Map.of(
                        "type", "subscribed",
                        "deviceId", deviceIdStr,
                        "userId", userIdStr,
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
        UUID userId = sessionUserMap.remove(sessionId);
        log.info("WebSocket connection closed: {} (device: {}, user: {})", sessionId, deviceId, userId);
    }

    public void broadcastNewMeasurement(UUID deviceId, Object measurementData) {
        log.info("Broadcasting measurement update for device: {}", deviceId);

        sessionDeviceMap.forEach((sessionId, subscribedDeviceId) -> {
            if (subscribedDeviceId.equals(deviceId)) {
                WebSocketSession session = sessions.get(sessionId);

                if (session != null && session.isOpen()) {
                    try {
                        Map<String, Object> message = Map.of(
                                "type", "newMeasurement",
                                "deviceId", deviceId.toString(),
                                "data", measurementData
                        );
                        String json = objectMapper.writeValueAsString(message);
                        session.sendMessage(new TextMessage(json));
                        log.info("Sent measurement to session {}", sessionId);
                    } catch (IOException e) {
                        log.error("Error sending measurement to session {}: {}", sessionId, e.getMessage());
                    }
                }
            }
        });
    }

    public void broadcastAlertToUser(UUID userId, Object alertData) {
        log.info("Broadcasting alert to user: {}", userId);

        sessionUserMap.forEach((sessionId, subscribedUserId) -> {
            if (subscribedUserId.equals(userId)) {
                WebSocketSession session = sessions.get(sessionId);

                if (session != null && session.isOpen()) {
                    try {
                        Map<String, Object> message = Map.of(
                                "type", "alert",
                                "data", alertData
                        );
                        String json = objectMapper.writeValueAsString(message);
                        session.sendMessage(new TextMessage(json));
                        log.info("Sent alert to user {} session {}", userId, sessionId);
                    } catch (IOException e) {
                        log.error("Error sending alert to session {}: {}", sessionId, e.getMessage());
                    }
                }
            }
        });
    }
}