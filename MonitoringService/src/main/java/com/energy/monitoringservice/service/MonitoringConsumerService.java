package com.energy.monitoringservice.service;

import com.energy.monitoringservice.config.RabbitMQConfig;
import com.energy.monitoringservice.dto.SensorDataDTO;
import com.energy.monitoringservice.dto.SyncMessageDTO;
import com.energy.monitoringservice.dto.WebSocketMessageDTO;
import com.energy.monitoringservice.entity.DeviceCache;
import com.energy.monitoringservice.entity.HourlyEnergyConsumption;
import com.energy.monitoringservice.entity.SensorMeasurement;
import com.energy.monitoringservice.repository.DeviceCacheRepository;
import com.energy.monitoringservice.repository.HourlyConsumptionRepository;
import com.energy.monitoringservice.repository.SensorMeasurementRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class MonitoringConsumerService {

    private static final Logger log = LoggerFactory.getLogger(MonitoringConsumerService.class);

    private final HourlyConsumptionRepository consumptionRepository;
    private final SensorMeasurementRepository measurementRepository;
    private final DeviceCacheRepository deviceCacheRepository;
    private final RabbitTemplate rabbitTemplate;

    @Value("${monitoring.replica.id:1}")
    private int replicaId;

    @Autowired
    public MonitoringConsumerService(HourlyConsumptionRepository consumptionRepository,
                                     SensorMeasurementRepository measurementRepository,
                                     DeviceCacheRepository deviceCacheRepository,
                                     RabbitTemplate rabbitTemplate) {
        this.consumptionRepository = consumptionRepository;
        this.measurementRepository = measurementRepository;
        this.deviceCacheRepository = deviceCacheRepository;
        this.rabbitTemplate = rabbitTemplate;
    }

    @RabbitListener(queues = "#{@ingestQueueName}")
    @Transactional
    public void processSensorData(SensorDataDTO sensorData) {
        log.info("[REPLICA {}] Received sensor data: deviceId={}, timestamp={}, value={} kWh",
                replicaId,
                sensorData.getDeviceId(),
                sensorData.getTimestamp(),
                sensorData.getMeasurementValue());

        if (!deviceCacheRepository.existsById(sensorData.getDeviceId())) {
            log.warn("[REPLICA {}] REJECTED sensor data for unknown device: {}. Device not found in cache.",
                    replicaId, sensorData.getDeviceId());
            return;
        }

        boolean exists = measurementRepository.existsByDeviceIdAndTimestamp(
                sensorData.getDeviceId(),
                sensorData.getTimestamp()
        );

        if (exists) {
            log.warn("[REPLICA {}] REJECTED duplicate: deviceId={}, timestamp={}",
                    replicaId,
                    sensorData.getDeviceId(),
                    sensorData.getTimestamp());
            return;
        }

        processValidSensorData(sensorData);

        LocalDateTime currentTimestamp = sensorData.getTimestamp();

        if (currentTimestamp.getMinute() == 0 && currentTimestamp.getSecond() == 0) {
            LocalDateTime previousHour = currentTimestamp.truncatedTo(ChronoUnit.HOURS).minusHours(1);
            UUID deviceId = sensorData.getDeviceId();

            consumptionRepository.findByDeviceIdAndHour(deviceId, previousHour)
                    .ifPresent(hourlyData -> {
                        try {
                            Map<String, Object> wsData = new HashMap<>();
                            wsData.put("hour", hourlyData.getHour().toString());
                            wsData.put("totalConsumption", hourlyData.getTotalConsumption());
                            wsData.put("deviceId", deviceId.toString());

                            log.info("[REPLICA {}] Broadcasting HOURLY AGGREGATE for device {} hour: {}",
                                    replicaId, deviceId, previousHour);
                            publishMeasurementUpdate(deviceId, wsData);
                            log.info("WebSocket broadcast completed for hour: {}", previousHour);

                        } catch (Exception e) {
                            log.error("[REPLICA {}] Failed to broadcast WebSocket message for {}: {}",
                                    replicaId, deviceId, e.getMessage(), e);                        }
                    });
        }
    }

    private void  processValidSensorData(SensorDataDTO sensorData) {
        SensorMeasurement measurement = new SensorMeasurement(
                sensorData.getDeviceId(),
                sensorData.getTimestamp(),
                sensorData.getMeasurementValue()
        );
        SensorMeasurement savedMeasurement = measurementRepository.save(measurement);

        log.info("[REPLICA {}] Saved measurement: id={}, deviceId={}, timestamp={}, value={} kWh",
                replicaId,
                savedMeasurement.getId(),
                savedMeasurement.getDeviceId(),
                savedMeasurement.getTimestamp(),
                savedMeasurement.getMeasurementValue());

        LocalDateTime currentTimestamp = sensorData.getTimestamp();
        if (currentTimestamp.getMinute() == 0 && currentTimestamp.getSecond() == 0) {
            LocalDateTime previousHour = currentTimestamp.truncatedTo(ChronoUnit.HOURS).minusHours(1);
            createHourlyAggregateForCompletedHour(sensorData.getDeviceId(), previousHour);
        }
    }

    private void createHourlyAggregateForCompletedHour(UUID deviceId, LocalDateTime hourTimestamp) {
        LocalDateTime hourStart = hourTimestamp;
        LocalDateTime hourEnd = hourTimestamp.plusHours(1);

        List<SensorMeasurement> measurements = measurementRepository.findByDeviceIdAndTimestampBetween(
                deviceId, hourStart, hourEnd);

        if (measurements.isEmpty()) {
            log.debug("[REPLICA {}] No measurements found for device {} in hour {}",
                    replicaId, deviceId, hourTimestamp);
            return;
        }

        double totalConsumption = measurements.stream()
                .mapToDouble(SensorMeasurement::getMeasurementValue)
                .sum();

        HourlyEnergyConsumption hourlyData = new HourlyEnergyConsumption();
        hourlyData.setDeviceId(deviceId);
        hourlyData.setHour(hourTimestamp);
        hourlyData.setTotalConsumption(totalConsumption);
        hourlyData.setCreatedAt(LocalDateTime.now());

        HourlyEnergyConsumption saved = consumptionRepository.save(hourlyData);

        log.info("[REPLICA {}] HOURLY AGGREGATE CREATED: device={}, hour={}, consumption={} kWh, measurements={}",
                replicaId, deviceId, hourTimestamp, totalConsumption, measurements.size());

        checkOverconsumptionForCompletedHour(deviceId, hourTimestamp, totalConsumption);
    }

    private void checkOverconsumptionForCompletedHour(UUID deviceId, LocalDateTime hourTimestamp, double totalConsumption) {
        DeviceCache deviceCache = deviceCacheRepository.findById(deviceId).orElse(null);

        if (deviceCache == null || deviceCache.getMaxConsumption() == null) {
            return;
        }

        Double maxConsumption = deviceCache.getMaxConsumption();

        if (totalConsumption > maxConsumption) {
            log.warn("[REPLICA {}] OVERCONSUMPTION! Device {}: {} kWh > {} kWh (hour {})",
                    replicaId, deviceId, totalConsumption, maxConsumption, hourTimestamp);

            Map<String, Object> alert = new HashMap<>();
            alert.put("type", "OVERCONSUMPTION");
            alert.put("deviceId", deviceId.toString());
            alert.put("deviceName", deviceCache.getDeviceName());
            alert.put("currentValue", totalConsumption);
            alert.put("maxConsumption", maxConsumption);
            alert.put("timestamp", hourTimestamp.toString());
            alert.put("message", String.format("Device %s exceeded maximum consumption in hour %s! Consumed: %.3f kWh, Max: %.3f kWh",
                    deviceCache.getDeviceName(), hourTimestamp, totalConsumption, maxConsumption));

            if (deviceCache.getUserId() != null) {
                publishAlert(deviceCache.getUserId(), deviceId, alert);
            } else {
                log.warn("Cannot send alert: device {} has no assigned user", deviceId);
            }
        }
    }

    private void publishAlert(UUID userId, UUID deviceId, Map<String, Object> alertData) {
        try {
            WebSocketMessageDTO message = new WebSocketMessageDTO();
            message.setType("ALERT");
            message.setUserId(userId);
            message.setDeviceId(deviceId);
            message.setData(alertData);

            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.WEBSOCKET_EXCHANGE,
                    "alert",
                    message
            );

            log.info("[REPLICA {}] Published ALERT: userId={}, deviceId={}", replicaId, userId, deviceId);
        } catch (Exception e) {
            log.error("[REPLICA {}] Failed to publish alert: {}", replicaId, e.getMessage(), e);
        }
    }

    private void publishMeasurementUpdate(UUID deviceId, Map<String, Object> measurementData) {
        try {
            WebSocketMessageDTO message = new WebSocketMessageDTO();
            message.setType("MEASUREMENT");
            message.setDeviceId(deviceId);
            message.setData(measurementData);

            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.WEBSOCKET_EXCHANGE,
                    "measurement",
                    message
            );

            log.info("[REPLICA {}] Published MEASUREMENT: deviceId={}", replicaId, deviceId);
        } catch (Exception e) {
            log.error("[REPLICA {}] Failed to publish measurement: {}", replicaId, e.getMessage(), e);
        }
    }

    @RabbitListener(queues = RabbitMQConfig.MONITORING_SYNC_QUEUE)
    @Transactional
    public void handleSyncMessage(SyncMessageDTO message) {
        log.info("[REPLICA {}] Received sync message: {}", replicaId, message.getEventType());

        try {
            switch (message.getEventType()) {
                case "DEVICE_CREATED":
                    handleDeviceCreated(message);
                    break;

                case "DEVICE_DELETED":
                    handleDeviceDeleted(message.getDeviceId());
                    break;

                case "DEVICE_ASSIGNED":
                    handleDeviceAssigned(message);
                    break;

                case "DEVICE_UNASSIGNED":
                    handleDeviceUnassigned(message.getDeviceId());
                    break;
                default:
                    log.debug("[REPLICA {}] Ignoring event: {}", replicaId, message.getEventType());
            }
        } catch (Exception e) {
            log.error("[REPLICA {}] Failed to process sync message: {}", replicaId, e.getMessage(), e);
            throw e;
        }
    }

    private void handleDeviceCreated(SyncMessageDTO message) {
        try {
            if (deviceCacheRepository.existsById(message.getDeviceId())) {
                log.info("[REPLICA {}] Device {} already in cache", replicaId, message.getDeviceId());
                return;
            }

            DeviceCache cache = new DeviceCache(
                    message.getDeviceId(),
                    message.getDeviceName(),
                    message.getMaxConsumption()
            );            deviceCacheRepository.save(cache);

            log.info("[REPLICA {}] Device {} added to cache", replicaId, message.getDeviceId());

        } catch (Exception e) {
            log.error("[REPLICA {}] Failed to add device {}: {}", replicaId, message.getDeviceId(), e.getMessage());
            throw e;
        }
    }

    private void handleDeviceAssigned(SyncMessageDTO message) {
        try {
            DeviceCache cache = deviceCacheRepository.findById(message.getDeviceId()).orElse(null);

            if (cache == null) {
                log.error("[REPLICA {}] Device {} not found", replicaId, message.getDeviceId());
                return;
            }

            cache.setUserId(message.getUserId());
            deviceCacheRepository.save(cache);

            log.info("[REPLICA {}] Device {} assigned to user {}", replicaId, message.getDeviceId(), message.getUserId());
        } catch (Exception e) {
            log.error("[REPLICA {}] Failed to assign device: {}", replicaId, e.getMessage());
            throw e;
        }
    }

    private void handleDeviceUnassigned(UUID deviceId) {
        try {
            DeviceCache cache = deviceCacheRepository.findById(deviceId).orElse(null);

            if (cache == null) {
                log.warn("[REPLICA {}] Device {} not found", replicaId, deviceId);
                return;
            }

            cache.setUserId(null);
            deviceCacheRepository.save(cache);

            log.info("[REPLICA {}] Device {} unassigned", replicaId, deviceId);
        } catch (Exception e) {
            log.error("[REPLICA {}] Failed to unassign device: {}", replicaId, e.getMessage());
            throw e;
        }
    }

    private void handleDeviceDeleted(UUID deviceId) {
        try {
            deviceCacheRepository.deleteById(deviceId);
            log.info("[REPLICA {}] Device {} removed from cache", replicaId, deviceId);

            int deletedMeasurements = measurementRepository.deleteByDeviceId(deviceId);
            log.info("[REPLICA {}] Deleted {} measurements for device {}", replicaId, deletedMeasurements, deviceId);

            int deletedAggregates = consumptionRepository.deleteByDeviceId(deviceId);
            log.info("[REPLICA {}] Deleted {} aggregates for device {}", replicaId, deletedAggregates, deviceId);
        } catch (Exception e) {
            log.error("[REPLICA {}] Failed to handle device deletion: {}", replicaId, e.getMessage());
            throw e;
        }
    }
}