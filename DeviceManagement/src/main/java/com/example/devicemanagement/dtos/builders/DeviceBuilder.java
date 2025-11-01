package com.example.devicemanagement.dtos.builders;

import com.example.devicemanagement.dtos.DeviceDTO;
import com.example.devicemanagement.dtos.DeviceDetailsDTO;
import com.example.devicemanagement.dtos.DeviceWithUserDTO;
import com.example.devicemanagement.entities.Device;

import java.util.UUID;

public class DeviceBuilder {

    private DeviceBuilder() {
    }

    public static DeviceDTO toDeviceDTO(Device device) {
        return new DeviceDTO(
                device.getId(),
                device.getName(),
                device.getDescription(),
                device.getMaxConsumption()
        );
    }

    public static DeviceDetailsDTO toDeviceDetailsDTO(Device device) {
        return new DeviceDetailsDTO(
                device.getId(),
                device.getName(),
                device.getDescription(),
                device.getMaxConsumption()
        );
    }

    public static DeviceWithUserDTO toDeviceWithUserDTO(Device device, UUID userId) {
        return new DeviceWithUserDTO(
                device.getId(),
                device.getName(),
                device.getDescription(),
                device.getMaxConsumption(),
                userId
        );
    }

    public static Device toEntity(DeviceDetailsDTO deviceDetailsDTO) {
        Device device = new Device(
                deviceDetailsDTO.getName(),
                deviceDetailsDTO.getDescription(),
                deviceDetailsDTO.getMaxConsumption()
        );
        return device;
    }
}