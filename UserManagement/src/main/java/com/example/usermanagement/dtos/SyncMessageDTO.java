package com.example.usermanagement.dtos;

import java.io.Serializable;
import java.util.UUID;

public class SyncMessageDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    private String eventType;
    private UUID userId;
    private UUID credentialsId;
    private UUID deviceId;
    private String username;
    private String password;
    private String email;
    private String fullName;
    private String role;
    private String deviceName;
    private Double maxConsumption;
    private String errorMessage;

    public SyncMessageDTO() {
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public UUID getCredentialsId() {
        return credentialsId;
    }

    public void setCredentialsId(UUID credentialsId) {
        this.credentialsId = credentialsId;
    }

    public UUID getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(UUID deviceId) {
        this.deviceId = deviceId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getDeviceName() {
        return deviceName;
    }

    public void setDeviceName(String deviceName) {
        this.deviceName = deviceName;
    }

    public Double getMaxConsumption() {
        return maxConsumption;
    }

    public void setMaxConsumption(Double maxConsumption) {
        this.maxConsumption = maxConsumption;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    @Override
    public String toString() {
        return "SyncMessageDTO{" +
                "eventType='" + eventType + '\'' +
                ", userId=" + userId +
                ", credentialsId=" + credentialsId +
                ", deviceId=" + deviceId +
                ", username='" + username + '\'' +
                ", email='" + email + '\'' +
                ", fullName='" + fullName + '\'' +
                ", role='" + role + '\'' +
                ", deviceName='" + deviceName + '\'' +
                ", maxConsumption=" + maxConsumption +
                ", errorMessage='" + errorMessage + '\'' +
                '}';
    }
}