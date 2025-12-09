package com.example.devicemanagement.services;

import com.example.devicemanagement.config.RabbitMQConfig;
import com.example.devicemanagement.dtos.DeviceDTO;
import com.example.devicemanagement.dtos.DeviceDetailsDTO;
import com.example.devicemanagement.dtos.DeviceWithUserDTO;
import com.example.devicemanagement.dtos.SyncMessageDTO;
import com.example.devicemanagement.dtos.builders.DeviceBuilder;
import com.example.devicemanagement.entities.Device;
import com.example.devicemanagement.entities.DeviceUserAssignment;
import com.example.devicemanagement.entities.UserCache;
import com.example.devicemanagement.handlers.exceptions.model.ResourceNotFoundException;
import com.example.devicemanagement.repositories.DeviceRepository;
import com.example.devicemanagement.repositories.DeviceUserAssignmentRepository;
import com.example.devicemanagement.repositories.UserCacheRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
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
    private final RabbitTemplate rabbitTemplate;


    @Autowired
    public DeviceService(DeviceRepository deviceRepository, DeviceUserAssignmentRepository assignmentRepository, UserCacheRepository userCacheRepository, RabbitTemplate rabbitTemplate) {
        this.deviceRepository = deviceRepository;
        this.assignmentRepository = assignmentRepository;
        this.userCacheRepository = userCacheRepository;
        this.rabbitTemplate = rabbitTemplate;
    }

    @RabbitListener(queues = RabbitMQConfig.DEVICE_SYNC_QUEUE)
    @Transactional
    public void handleSyncMessage(SyncMessageDTO message) {
        LOGGER.info("Device received sync message: {}", message.getEventType());

        try {
            switch (message.getEventType()) {
                case "USER_ID_ASSIGNED":
                    handleUserIdAssigned(message);
                    break;
                case "USER_CREATE_FAILED":
                    handleUserCreateFailed(message);
                    break;
                case "USER_DELETED":
                    handleUserDeleted(message);
                    break;
                case "DEVICE_CREATED":
                case "DEVICE_DELETED":
                    LOGGER.debug("Ignoring device event echo: {}", message.getEventType());
                    break;
                case "USER_CREATED":
                    LOGGER.debug("Ignoring USER_CREATED, waiting for USER_ID_ASSIGNED");
                    break;
                default:
                    LOGGER.debug("Ignoring event: {}", message.getEventType());
            }
        } catch (Exception e) {
            LOGGER.error("Failed to process sync message: {}", e.getMessage(), e);

            if ("USER_ID_ASSIGNED".equals(message.getEventType())) {
                publishUserCacheFailed(message.getUserId(), e.getMessage());
            }
        }
    }

    private void handleUserIdAssigned(SyncMessageDTO message) {
        LOGGER.info("Received USER_ID_ASSIGNED: userId={}, username={}",
                message.getUserId(), message.getUsername());

        try {
            if (userCacheRepository.existsById(message.getUserId())) {
                LOGGER.info("User {} already in cache (idempotent operation)", message.getUserId());
                return;
            }

            UserCache cache = new UserCache();
            cache.setUserId(message.getUserId());
            userCacheRepository.save(cache);

            LOGGER.info("User {} ({}) added to cache", message.getUserId(), message.getUsername());

        } catch (Exception e) {
            LOGGER.error("Failed to add user {} to cache: {}", message.getUserId(), e.getMessage());
            throw e;
        }
    }

    private void handleUserDeleted(SyncMessageDTO message) {
        LOGGER.info("Received USER_DELETED: userId={}", message.getUserId());

        try {
            userCacheRepository.deleteById(message.getUserId());
            LOGGER.info("User {} removed from cache", message.getUserId());

            assignmentRepository.deleteByUserId(message.getUserId());
            LOGGER.info("Deleted all device assignments for user {}", message.getUserId());

        } catch (Exception e) {
            LOGGER.error("Failed to handle user deletion for {}: {}", message.getUserId(), e.getMessage());
        }
    }

    private void handleUserCreateFailed(SyncMessageDTO message) {
        LOGGER.error("Received USER_CREATE_FAILED: userId={}, credentialsId={}, error={}",
                message.getUserId(), message.getCredentialsId(), message.getErrorMessage());

        try {
            if (message.getUserId() != null && userCacheRepository.existsById(message.getUserId())) {
                userCacheRepository.deleteById(message.getUserId());
                LOGGER.info("Rolled back user cache for failed registration: {}", message.getUserId());
            }
        } catch (Exception e) {
            LOGGER.error("Failed to rollback user cache {}: {}", message.getUserId(), e.getMessage());
        }
    }

    private void publishUserCacheFailed(UUID userId, String errorMessage) {
        try {
            SyncMessageDTO message = new SyncMessageDTO();
            message.setEventType("USER_CREATE_FAILED");
            message.setUserId(userId);
            message.setErrorMessage("Failed to cache user in Device Service: " + errorMessage);

            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.SYNC_EXCHANGE,
                    "",
                    message
            );

            LOGGER.error("Published USER_CREATE_FAILED for userId: {}", userId);
        } catch (Exception e) {
            LOGGER.error("Failed to publish USER_CREATE_FAILED: {}", e.getMessage());
        }
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

        publishDeviceCreatedEvent(device.getId(), device.getName(), device.getMaxConsumption());

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

        publishDeviceDeletedEvent(id);
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

        publishDeviceAssignedEvent(deviceId, userId);
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

        publishDeviceUnassignedEvent(deviceId);
    }

    private void publishDeviceAssignedEvent(UUID deviceId, UUID userId) {
        try {
            SyncMessageDTO syncMessage = new SyncMessageDTO();
            syncMessage.setEventType("DEVICE_ASSIGNED");
            syncMessage.setDeviceId(deviceId);
            syncMessage.setUserId(userId);

            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.SYNC_EXCHANGE,
                    "",
                    syncMessage
            );

            LOGGER.info("Published DEVICE_ASSIGNED event: deviceId={}, userId={}", deviceId, userId);
        } catch (Exception e) {
            LOGGER.error("Failed to publish DEVICE_ASSIGNED event: {}", e.getMessage());
        }
    }

    private void publishDeviceUnassignedEvent(UUID deviceId) {
        try {
            SyncMessageDTO syncMessage = new SyncMessageDTO();
            syncMessage.setEventType("DEVICE_UNASSIGNED");
            syncMessage.setDeviceId(deviceId);

            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.SYNC_EXCHANGE,
                    "",
                    syncMessage
            );

            LOGGER.info("Published DEVICE_UNASSIGNED event for deviceId: {}", deviceId);
        } catch (Exception e) {
            LOGGER.error("Failed to publish DEVICE_UNASSIGNED event: {}", e.getMessage());
        }
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

    private void publishDeviceCreatedEvent(UUID deviceId, String deviceName, Double maxConsumption) {
        try {
            SyncMessageDTO syncMessage = new SyncMessageDTO();
            syncMessage.setEventType("DEVICE_CREATED");
            syncMessage.setDeviceId(deviceId);
            syncMessage.setDeviceName(deviceName);
            syncMessage.setMaxConsumption(maxConsumption);

            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.SYNC_EXCHANGE,
                    "",
                    syncMessage
            );

            LOGGER.info("Published DEVICE_CREATED event for deviceId: {}", deviceId);
        } catch (Exception e) {
            LOGGER.error("Failed to publish DEVICE_CREATED event: {}", e.getMessage());
        }
    }


    private void publishDeviceDeletedEvent(UUID deviceId) {
        try {
            SyncMessageDTO syncMessage = new SyncMessageDTO();
            syncMessage.setEventType("DEVICE_DELETED");
            syncMessage.setDeviceId(deviceId);

            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.SYNC_EXCHANGE,
                    "",
                    syncMessage
            );

            LOGGER.info("Published DEVICE_DELETED event for deviceId: {}", deviceId);
        } catch (Exception e) {
            LOGGER.error("Failed to publish DEVICE_DELETED event: {}", e.getMessage());
        }
    }
}