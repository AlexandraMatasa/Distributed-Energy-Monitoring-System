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

    public DeviceCache() {
    }

    public DeviceCache(UUID deviceId) {
        this.deviceId = deviceId;
    }

    public UUID getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(UUID deviceId) {
        this.deviceId = deviceId;
    }

    @Override
    public String toString() {
        return "DeviceCache{" +
                "deviceId=" + deviceId +
                '}';
    }
}