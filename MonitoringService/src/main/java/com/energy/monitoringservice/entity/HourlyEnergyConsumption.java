package com.energy.monitoringservice.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "hourly_energy_consumption",
        indexes = {
                @Index(name = "idx_device_hour", columnList = "device_id,hour")
        })
public class HourlyEnergyConsumption implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "device_id", nullable = false)
    @JdbcTypeCode(SqlTypes.UUID)
    private UUID deviceId;

    @Column(nullable = false)
    private LocalDateTime hour;

    @Column(name = "total_consumption", nullable = false)
    private Double totalConsumption;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public HourlyEnergyConsumption() {
    }

    public HourlyEnergyConsumption(Long id, UUID deviceId, LocalDateTime hour,
                                   Double totalConsumption, LocalDateTime createdAt) {
        this.id = id;
        this.deviceId = deviceId;
        this.hour = hour;
        this.totalConsumption = totalConsumption;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public UUID getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(UUID deviceId) {
        this.deviceId = deviceId;
    }

    public LocalDateTime getHour() {
        return hour;
    }

    public void setHour(LocalDateTime hour) {
        this.hour = hour;
    }

    public Double getTotalConsumption() {
        return totalConsumption;
    }

    public void setTotalConsumption(Double totalConsumption) {
        this.totalConsumption = totalConsumption;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public String toString() {
        return "HourlyEnergyConsumption{" +
                "id=" + id +
                ", deviceId=" + deviceId +
                ", hour=" + hour +
                ", totalConsumption=" + totalConsumption +
                ", createdAt=" + createdAt +
                '}';
    }
}