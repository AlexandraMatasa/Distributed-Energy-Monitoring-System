package com.energy.communicationservice.dto;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ChatSessionDTO {
    private UUID userId;
    private String username;
    private String sessionId;
    private boolean humanHandoffRequested;
    private LocalDateTime lastMessageTime;
    private List<ChatMessageDTO> conversationHistory;
    private int unreadAdminCount;

    public ChatSessionDTO() {
        this.conversationHistory = new ArrayList<>();
        this.lastMessageTime = LocalDateTime.now();
        this.humanHandoffRequested = false;
        this.unreadAdminCount = 0;
    }

    public ChatSessionDTO(UUID userId, String username, String sessionId) {
        this.userId = userId;
        this.username = username;
        this.sessionId = sessionId;
        this.conversationHistory = new ArrayList<>();
        this.lastMessageTime = LocalDateTime.now();
        this.humanHandoffRequested = false;
        this.unreadAdminCount = 0;
    }

    public void addMessage(ChatMessageDTO message) {
        this.conversationHistory.add(message);
        this.lastMessageTime = LocalDateTime.now();

        // Count unread messages from client
        if ("CLIENT".equals(message.getRole())) {
            this.unreadAdminCount++;
        }
    }

    public void markAsRead() {
        this.unreadAdminCount = 0;
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public boolean isHumanHandoffRequested() {
        return humanHandoffRequested;
    }

    public void setHumanHandoffRequested(boolean humanHandoffRequested) {
        this.humanHandoffRequested = humanHandoffRequested;
    }

    public LocalDateTime getLastMessageTime() {
        return lastMessageTime;
    }

    public void setLastMessageTime(LocalDateTime lastMessageTime) {
        this.lastMessageTime = lastMessageTime;
    }

    public List<ChatMessageDTO> getConversationHistory() {
        return conversationHistory;
    }

    public void setConversationHistory(List<ChatMessageDTO> conversationHistory) {
        this.conversationHistory = conversationHistory;
    }

    public int getUnreadAdminCount() {
        return unreadAdminCount;
    }

    public void setUnreadAdminCount(int unreadAdminCount) {
        this.unreadAdminCount = unreadAdminCount;
    }
}