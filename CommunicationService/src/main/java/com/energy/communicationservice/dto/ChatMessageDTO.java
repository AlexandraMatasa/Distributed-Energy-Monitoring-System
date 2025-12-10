package com.energy.communicationservice.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public class ChatMessageDTO {
    private UUID userId;
    private String username;
    private String role; // "CLIENT", "ADMIN", "BOT"
    private String message;
    private LocalDateTime timestamp;
    private String sessionId;

    public ChatMessageDTO() {}

    public ChatMessageDTO(UUID userId, String username, String role, String message, String sessionId) {
        this.userId = userId;
        this.username = username;
        this.role = role;
        this.message = message;
        this.timestamp = LocalDateTime.now();
        this.sessionId = sessionId;
    }

    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }

    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }
}