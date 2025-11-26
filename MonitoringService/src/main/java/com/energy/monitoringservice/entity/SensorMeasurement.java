package com.energy.monitoringservice.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "sensor_measurements",
        indexes = {
                @Index(name = "idx_sensor_device_timestamp", columnList = "device_id,timestamp")
        })
public class SensorMeasurement implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "device_id", nullable = false)
    @JdbcTypeCode(SqlTypes.UUID)
    private UUID deviceId;

    @Column(nullable = false)
    private LocalDateTime timestamp;

    @Column(name = "measurement_value", nullable = false)
    private Double measurementValue;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public SensorMeasurement() {
    }

    public SensorMeasurement(UUID deviceId, LocalDateTime timestamp, Double measurementValue) {
        this.deviceId = deviceId;
        this.timestamp = timestamp;
        this.measurementValue = measurementValue;
        this.createdAt = LocalDateTime.now();
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

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public String toString() {
        return "SensorMeasurement{" +
                "id=" + id +
                ", deviceId=" + deviceId +
                ", timestamp=" + timestamp +
                ", measurementValue=" + measurementValue +
                ", createdAt=" + createdAt +
                '}';
    }
}