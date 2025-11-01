package com.example.devicemanagement.dtos;

import java.util.Objects;
import java.util.UUID;

public class DeviceWithUserDTO {

    private UUID id;
    private String name;
    private String description;
    private Double maxConsumption;
    private UUID userId;

    public DeviceWithUserDTO() {
    }

    public DeviceWithUserDTO(UUID id, String name, String description, Double maxConsumption, UUID userId) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.maxConsumption = maxConsumption;
        this.userId = userId;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
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
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DeviceWithUserDTO that = (DeviceWithUserDTO) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}