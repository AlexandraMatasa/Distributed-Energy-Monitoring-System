package com.energy.communicationservice.handler;

import com.energy.communicationservice.dto.ChatMessageDTO;
import com.energy.communicationservice.dto.ChatSessionDTO;
import com.energy.communicationservice.service.ChatService;
import com.energy.communicationservice.service.ChatSessionManager;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class ChatWebSocketHandler extends TextWebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(ChatWebSocketHandler.class);

    private final Map<String, WebSocketSession> clientSessions = new ConcurrentHashMap<>();
    private final Map<String, WebSocketSession> adminSessions = new ConcurrentHashMap<>();
    private final Map<String, UUID> sessionUserMap = new ConcurrentHashMap<>();
    private final Map<UUID, String> userSessionMap = new ConcurrentHashMap<>();

    private final ObjectMapper objectMapper;
    private final ChatService chatService;
    private final ChatSessionManager sessionManager;

    @Autowired
    public ChatWebSocketHandler(ChatService chatService, ChatSessionManager sessionManager) {
        this.chatService = chatService;
        this.sessionManager = sessionManager;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        log.info("Chat WebSocket connected: {}", session.getId());
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String payload = message.getPayload();
        log.info("Received chat message from {}: {}", session.getId(), payload);

        try {
            Map<String, Object> data = objectMapper.readValue(payload, Map.class);
            String action = (String) data.get("action");

            if ("register".equals(action)) {
                handleRegister(session, data);
            } else if ("message".equals(action)) {
                handleMessage(session, data);
            } else if ("get_sessions".equals(action)) {
                handleGetSessions(session);
            } else if ("get_conversation".equals(action)) {
                handleGetConversation(session, data);
            } else if ("mark_read".equals(action)) {
                handleMarkRead(session, data);
            }

        } catch (Exception e) {
            log.error("Error handling chat message: {}", e.getMessage(), e);
            sendError(session, "Failed to process message");
        }
    }

    private void handleRegister(WebSocketSession session, Map<String, Object> data) throws IOException {
        String userIdStr = (String) data.get("userId");
        String role = (String) data.get("role");
        String username = (String) data.get("username");

        UUID userId = UUID.fromString(userIdStr);

        sessionUserMap.put(session.getId(), userId);
        userSessionMap.put(userId, session.getId());

        if ("ADMIN".equals(role)) {
            adminSessions.put(session.getId(), session);
            log.info("Registered ADMIN session: {} for user: {}", session.getId(), username);

            sendSessionsList(session);
        } else {
            clientSessions.put(session.getId(), session);
            log.info("Registered CLIENT session: {} for user: {}", session.getId(), username);
        }

        Map<String, Object> response = Map.of(
                "type", "registered",
                "message", "Successfully registered for chat",
                "role", role
        );
        session.sendMessage(new TextMessage(objectMapper.writeValueAsString(response)));
    }

    private void handleMessage(WebSocketSession session, Map<String, Object> data) throws IOException {
        String messageText = (String) data.get("message");
        String userIdStr = (String) data.get("userId");
        String username = (String) data.get("username");
        String role = (String) data.get("role");

        UUID userId = UUID.fromString(userIdStr);

        ChatMessageDTO userMessage = new ChatMessageDTO(
                userId,
                username,
                role,
                messageText,
                session.getId()
        );

        sendMessageToSession(session, userMessage);

        if ("CLIENT".equals(role)) {
            ChatMessageDTO botResponse = chatService.processUserMessage(userMessage);

            if (botResponse != null) {
                sendMessageToSession(session, botResponse);

                notifyAdminsOfNewMessage(userId);
            } else {
                forwardToAdmins(userMessage);

                broadcastSessionsToAdmins();
            }
        } else if ("ADMIN".equals(role)) {
            String targetUserIdStr = (String) data.get("targetUserId");
            if (targetUserIdStr != null) {
                UUID targetUserId = UUID.fromString(targetUserIdStr);

                sessionManager.addMessageToSession(targetUserId, userMessage);

                sendMessageToUser(targetUserId, userMessage);

                broadcastSessionsToAdmins();
            }
        }
    }

    private void handleGetSessions(WebSocketSession session) throws IOException {
        sendSessionsList(session);
    }

    private void handleGetConversation(WebSocketSession session, Map<String, Object> data) throws IOException {
        String userIdStr = (String) data.get("userId");
        UUID userId = UUID.fromString(userIdStr);

        ChatSessionDTO chatSession = sessionManager.getSession(userId);
        if (chatSession != null) {
            Map<String, Object> response = Map.of(
                    "type", "conversation_history",
                    "data", chatSession
            );
            session.sendMessage(new TextMessage(objectMapper.writeValueAsString(response)));
        } else {
            sendError(session, "Session not found");
        }
    }

    private void handleMarkRead(WebSocketSession session, Map<String, Object> data) throws IOException {
        String userIdStr = (String) data.get("userId");
        UUID userId = UUID.fromString(userIdStr);

        sessionManager.markSessionAsRead(userId);

        broadcastSessionsToAdmins();
    }

    private void sendSessionsList(WebSocketSession session) throws IOException {
        List<ChatSessionDTO> sessions = sessionManager.getHumanHandoffSessions();
        Map<String, Object> response = Map.of(
                "type", "sessions_list",
                "data", sessions
        );
        session.sendMessage(new TextMessage(objectMapper.writeValueAsString(response)));
        log.info("Sent {} active sessions to admin", sessions.size());
    }

    private void broadcastSessionsToAdmins() {
        log.info("Broadcasting sessions list to {} admin(s)", adminSessions.size());
        for (WebSocketSession adminSession : adminSessions.values()) {
            try {
                sendSessionsList(adminSession);
            } catch (IOException e) {
                log.error("Error broadcasting to admin: {}", e.getMessage());
            }
        }
    }

    private void notifyAdminsOfNewMessage(UUID userId) {
        log.info("Notifying admins of new message from user: {}", userId);
        broadcastSessionsToAdmins();
    }

    private void sendMessageToSession(WebSocketSession session, ChatMessageDTO chatMessage) {
        if (session != null && session.isOpen()) {
            try {
                Map<String, Object> response = Map.of(
                        "type", "chat_message",
                        "data", chatMessage
                );
                session.sendMessage(new TextMessage(objectMapper.writeValueAsString(response)));
            } catch (IOException e) {
                log.error("Error sending message to session {}: {}", session.getId(), e.getMessage());
            }
        }
    }

    private void sendMessageToUser(UUID userId, ChatMessageDTO chatMessage) {
        String sessionId = userSessionMap.get(userId);
        if (sessionId != null) {
            WebSocketSession session = clientSessions.get(sessionId);
            sendMessageToSession(session, chatMessage);
        } else {
            log.warn("No active session for user: {}", userId);
        }
    }

    private void forwardToAdmins(ChatMessageDTO userMessage) {
        log.info("Forwarding message to {} admin(s)", adminSessions.size());
        for (WebSocketSession adminSession : adminSessions.values()) {
            sendMessageToSession(adminSession, userMessage);
        }
    }

    private void sendError(WebSocketSession session, String errorMessage) {
        try {
            Map<String, Object> error = Map.of(
                    "type", "error",
                    "message", errorMessage
            );
            session.sendMessage(new TextMessage(objectMapper.writeValueAsString(error)));
        } catch (IOException e) {
            log.error("Error sending error message: {}", e.getMessage());
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        String sessionId = session.getId();
        UUID userId = sessionUserMap.remove(sessionId);

        clientSessions.remove(sessionId);
        boolean wasAdmin = adminSessions.remove(sessionId) != null;

        if (userId != null) {
            userSessionMap.remove(userId);
        }

        log.info("Chat WebSocket closed: {} (user: {}, was admin: {})", sessionId, userId, wasAdmin);
    }
}