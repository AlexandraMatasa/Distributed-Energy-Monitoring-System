package com.energy.loadbalancerservice.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public class SensorDataDTO {
    private UUID deviceId;
    private LocalDateTime timestamp;
    private Double measurementValue;

    public SensorDataDTO() {
    }

    public SensorDataDTO(UUID deviceId, LocalDateTime timestamp, Double measurementValue) {
        this.deviceId = deviceId;
        this.timestamp = timestamp;
        this.measurementValue = measurementValue;
    }

    public UUID getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(UUID deviceId) {
        this.deviceId = deviceId;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public Double getMeasurementValue() {
        return measurementValue;
    }

    public void setMeasurementValue(Double measurementValue) {
        this.measurementValue = measurementValue;
    }

    @Override
    public String toString() {
        return "SensorDataDTO{" +
                "deviceId=" + deviceId +
                ", timestamp=" + timestamp +
                ", measurementValue=" + measurementValue +
                '}';
    }
}