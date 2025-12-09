package com.energy.communicationservice.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;
import java.util.UUID;

public class WebSocketMessageDTO {

    @JsonProperty("type")
    private String type; // "ALERT", "MEASUREMENT", "CHAT"

    @JsonProperty("userId")
    private UUID userId;

    @JsonProperty("deviceId")
    private UUID deviceId;

    @JsonProperty("data")
    private Map<String, Object> data;

    public WebSocketMessageDTO() {}

    public WebSocketMessageDTO(String type, UUID userId, UUID deviceId, Map<String, Object> data) {
        this.type = type;
        this.userId = userId;
        this.deviceId = deviceId;
        this.data = data;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public UUID getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(UUID deviceId) {
        this.deviceId = deviceId;
    }

    public Map<String, Object> getData() {
        return data;
    }

    public void setData(Map<String, Object> data) {
        this.data = data;
    }
}