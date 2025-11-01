package com.example.devicemanagement.dtos;

import java.util.Objects;
import java.util.UUID;

public class DeviceDTO {
    private UUID id;
    private String name;
    private String description;
    private Double maxConsumption;

    public DeviceDTO() {}

    public DeviceDTO(UUID id, String name, String description, Double maxConsumption) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.maxConsumption = maxConsumption;
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DeviceDTO deviceDTO = (DeviceDTO) o;
        return Objects.equals(name, deviceDTO.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }
}
