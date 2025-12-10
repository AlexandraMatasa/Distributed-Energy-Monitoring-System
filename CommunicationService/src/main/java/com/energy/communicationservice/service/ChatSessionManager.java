package com.energy.communicationservice.service;

import com.energy.communicationservice.dto.ChatMessageDTO;
import com.energy.communicationservice.dto.ChatSessionDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
public class ChatSessionManager {

    private static final Logger log = LoggerFactory.getLogger(ChatSessionManager.class);

    private final Map<UUID, ChatSessionDTO> activeSessions = new ConcurrentHashMap<>();

    public ChatSessionDTO getOrCreateSession(UUID userId, String username, String sessionId) {
        return activeSessions.computeIfAbsent(userId, k -> {
            log.info("Creating new chat session for user: {} ({})", username, userId);
            return new ChatSessionDTO(userId, username, sessionId);
        });
    }

    public ChatSessionDTO getSession(UUID userId) {
        return activeSessions.get(userId);
    }

    public void addMessageToSession(UUID userId, ChatMessageDTO message) {
        ChatSessionDTO session = activeSessions.get(userId);
        if (session != null) {
            session.addMessage(message);
            log.info("Added message to session for user: {}", userId);
        }
    }

    public void enableHumanHandoff(UUID userId) {
        ChatSessionDTO session = activeSessions.get(userId);
        if (session != null) {
            session.setHumanHandoffRequested(true);
            log.info("Human handoff ENABLED for user: {}", userId);
        }
    }

    public boolean isHumanHandoffActive(UUID userId) {
        ChatSessionDTO session = activeSessions.get(userId);
        return session != null && session.isHumanHandoffRequested();
    }

    public List<ChatSessionDTO> getHumanHandoffSessions() {
        return activeSessions.values().stream()
                .filter(ChatSessionDTO::isHumanHandoffRequested)
                .sorted((s1, s2) -> s2.getLastMessageTime().compareTo(s1.getLastMessageTime()))
                .collect(Collectors.toList());
    }

    public void markSessionAsRead(UUID userId) {
        ChatSessionDTO session = activeSessions.get(userId);
        if (session != null) {
            session.markAsRead();
            log.info("Marked session as read for user: {}", userId);
        }
    }

    public void removeSession(UUID userId) {
        ChatSessionDTO removed = activeSessions.remove(userId);
        if (removed != null) {
            log.info("Removed chat session for user: {}", userId);
        }
    }

    public int getActiveSessionCount() {
        return activeSessions.size();
    }

    public int getHumanHandoffCount() {
        return (int) activeSessions.values().stream()
                .filter(ChatSessionDTO::isHumanHandoffRequested)
                .count();
    }
}