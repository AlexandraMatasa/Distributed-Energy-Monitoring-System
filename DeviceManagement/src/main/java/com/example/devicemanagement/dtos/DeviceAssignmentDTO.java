package com.example.devicemanagement.dtos;

import jakarta.validation.constraints.NotNull;

import java.util.Objects;
import java.util.UUID;

public class DeviceAssignmentDTO {

    @NotNull(message = "Device ID is required for assignment")
    private UUID deviceId;

    @NotNull(message = "User ID is required for assignment")
    private UUID userId;

    public DeviceAssignmentDTO() {
    }

    public DeviceAssignmentDTO(UUID deviceId, UUID userId) {
        this.deviceId = deviceId;
        this.userId = userId;
    }

    public UUID getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(UUID deviceId) {
        this.deviceId = deviceId;
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DeviceAssignmentDTO that = (DeviceAssignmentDTO) o;
        return Objects.equals(deviceId, that.deviceId) && Objects.equals(userId, that.userId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(deviceId, userId);
    }
}