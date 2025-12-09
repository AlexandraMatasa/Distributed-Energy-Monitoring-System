package com.energy.monitoringservice.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.io.Serializable;
import java.util.UUID;

@Entity
@Table(name = "device_cache")
public class DeviceCache implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @JdbcTypeCode(SqlTypes.UUID)
    @Column(name = "device_id")
    private UUID deviceId;

    @Column(name = "device_name")
    private String deviceName;

    @Column(name = "max_consumption")
    private Double maxConsumption;

    @Column(name = "user_id")
    @JdbcTypeCode(SqlTypes.UUID)
    private UUID userId;

    public DeviceCache() {
    }

    public DeviceCache(UUID deviceId, String deviceName, Double maxConsumption) {
        this.deviceId = deviceId;
        this.deviceName = deviceName;
        this.maxConsumption = maxConsumption;
    }

    public UUID getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(UUID deviceId) {
        this.deviceId = deviceId;
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

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    @Override
    public String toString() {
        return "DeviceCache{" +
                "deviceId=" + deviceId +
                ", deviceName='" + deviceName + '\'' +
                ", maxConsumption=" + maxConsumption +
                ", userId=" + userId +
                '}';
    }
}