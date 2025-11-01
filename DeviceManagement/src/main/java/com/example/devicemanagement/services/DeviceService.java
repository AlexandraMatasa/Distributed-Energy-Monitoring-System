package com.example.devicemanagement.services;

import com.example.devicemanagement.dtos.DeviceDTO;
import com.example.devicemanagement.dtos.DeviceDetailsDTO;
import com.example.devicemanagement.dtos.DeviceWithUserDTO;
import com.example.devicemanagement.dtos.builders.DeviceBuilder;
import com.example.devicemanagement.entities.Device;
import com.example.devicemanagement.entities.DeviceUserAssignment;
import com.example.devicemanagement.handlers.exceptions.model.ResourceNotFoundException;
import com.example.devicemanagement.repositories.DeviceRepository;
import com.example.devicemanagement.repositories.DeviceUserAssignmentRepository;
import com.example.devicemanagement.repositories.UserCacheRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class DeviceService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DeviceService.class);
    private final DeviceRepository deviceRepository;
    private final DeviceUserAssignmentRepository assignmentRepository;
    private final UserCacheRepository userCacheRepository;

    @Autowired
    public DeviceService(DeviceRepository deviceRepository, DeviceUserAssignmentRepository assignmentRepository, UserCacheRepository userCacheRepository) {
        this.deviceRepository = deviceRepository;
        this.assignmentRepository = assignmentRepository;
        this.userCacheRepository = userCacheRepository;
    }

    public List<DeviceDTO> findDevices() {
        List<Device> deviceList = deviceRepository.findAll();
        return deviceList.stream()
                .map(DeviceBuilder::toDeviceDTO)
                .collect(Collectors.toList());
    }

    public DeviceDetailsDTO findDeviceById(UUID id) {
        Optional<Device> deviceOptional = deviceRepository.findById(id);
        if (!deviceOptional.isPresent()) {
            LOGGER.error("Device with id {} was not found in db", id);
            throw new ResourceNotFoundException(Device.class.getSimpleName() + " with id: " + id);
        }
        return DeviceBuilder.toDeviceDetailsDTO(deviceOptional.get());
    }

    public UUID insert(DeviceDetailsDTO deviceDTO) {
        Device device = DeviceBuilder.toEntity(deviceDTO);
        device = deviceRepository.save(device);
        LOGGER.debug("Device with id {} was inserted in db", device.getId());
        return device.getId();
    }

    public UUID update(UUID id, DeviceDetailsDTO deviceDTO) {
        Optional<Device> deviceOptional = deviceRepository.findById(id);
        if (!deviceOptional.isPresent()) {
            LOGGER.error("Device with id {} was not found in db", id);
            throw new ResourceNotFoundException(Device.class.getSimpleName() + " with id: " + id);
        }

        Device device = deviceOptional.get();
        device.setName(deviceDTO.getName());
        device.setDescription(deviceDTO.getDescription());
        device.setMaxConsumption(deviceDTO.getMaxConsumption());

        deviceRepository.save(device);
        LOGGER.debug("Device with id {} was updated in db", id);
        return id;
    }

    @Transactional
    public void delete(UUID id) {
        Optional<Device> deviceOptional = deviceRepository.findById(id);
        if (!deviceOptional.isPresent()) {
            LOGGER.error("Device with id {} was not found in db", id);
            throw new ResourceNotFoundException(Device.class.getSimpleName() + " with id: " + id);
        }
        assignmentRepository.deleteByDeviceId(id);

        deviceRepository.deleteById(id);

        LOGGER.debug("Device with id {} was deleted from db", id);
    }

    @Transactional
    public void assignDeviceToUser(UUID deviceId, UUID userId) {
        Optional<Device> deviceOptional = deviceRepository.findById(deviceId);
        if (!deviceOptional.isPresent()) {
            LOGGER.error("Device with id {} was not found", deviceId);
            throw new ResourceNotFoundException("Device with id: " + deviceId);
        }

        if (!userCacheRepository.existsById(userId)) {
            LOGGER.error("User with id {} was not found in cache", userId);
            throw new ResourceNotFoundException("User with id: " + userId);
        }

        assignmentRepository.deleteByDeviceId(deviceId);

        DeviceUserAssignment assignment = new DeviceUserAssignment();
        assignment.setDeviceId(deviceId);
        assignment.setUserId(userId);
        assignmentRepository.save(assignment);

        LOGGER.debug("Device {} was assigned to user {}", deviceId, userId);
    }

    @Transactional
    public void unassignDevice(UUID deviceId) {
        Optional<Device> deviceOptional = deviceRepository.findById(deviceId);
        if (!deviceOptional.isPresent()) {
            LOGGER.error("Device with id {} was not found", deviceId);
            throw new ResourceNotFoundException("Device with id: " + deviceId);
        }

        assignmentRepository.deleteByDeviceId(deviceId);

        LOGGER.debug("Device {} was unassigned", deviceId);
    }

    public List<DeviceWithUserDTO> findDevicesByUserId(UUID userId) {
        List<DeviceUserAssignment> assignments = assignmentRepository.findByUserId(userId);

        if (assignments.isEmpty()) {
            return List.of();
        }

        List<UUID> deviceIds = assignments.stream()
                .map(DeviceUserAssignment::getDeviceId)
                .collect(Collectors.toList());

        List<Device> devices = deviceRepository.findAllById(deviceIds);

        return devices.stream()
                .map(device -> DeviceBuilder.toDeviceWithUserDTO(device, userId))
                .collect(Collectors.toList());
    }
}