package com.energy.monitoringservice.service;

import com.energy.monitoringservice.config.RabbitMQConfig;
import com.energy.monitoringservice.dto.SensorDataDTO;
import com.energy.monitoringservice.dto.SyncMessageDTO;
import com.energy.monitoringservice.entity.DeviceCache;
import com.energy.monitoringservice.entity.HourlyEnergyConsumption;
import com.energy.monitoringservice.entity.SensorMeasurement;
import com.energy.monitoringservice.repository.DeviceCacheRepository;
import com.energy.monitoringservice.repository.HourlyConsumptionRepository;
import com.energy.monitoringservice.repository.SensorMeasurementRepository;
import com.energy.monitoringservice.websocket.MonitoringWebSocketHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
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
    private final MonitoringWebSocketHandler webSocketHandler;

    @Autowired
    public MonitoringConsumerService(HourlyConsumptionRepository consumptionRepository,
                                     SensorMeasurementRepository measurementRepository,
                                     DeviceCacheRepository deviceCacheRepository,
                                     MonitoringWebSocketHandler webSocketHandler) {
        this.consumptionRepository = consumptionRepository;
        this.measurementRepository = measurementRepository;
        this.deviceCacheRepository = deviceCacheRepository;
        this.webSocketHandler = webSocketHandler;
    }

    @RabbitListener(queues = RabbitMQConfig.SENSOR_DATA_QUEUE)
    @Transactional
    public void processSensorData(SensorDataDTO sensorData) {
        log.info("Received sensor data: deviceId={}, timestamp={}, value={} kWh",
                sensorData.getDeviceId(),
                sensorData.getTimestamp(),
                sensorData.getMeasurementValue());

        if (!deviceCacheRepository.existsById(sensorData.getDeviceId())) {
            log.warn("REJECTED sensor data for unknown device: {}. Device not found in cache.",
                    sensorData.getDeviceId());
            return;
        }

        boolean exists = measurementRepository.existsByDeviceIdAndTimestamp(
                sensorData.getDeviceId(),
                sensorData.getTimestamp()
        );

        if (exists) {
            log.warn("REJECTED duplicate: deviceId={}, timestamp={}",
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
                            wsData.put("deviceId", deviceId.toString()); // Adaugă deviceId pentru filtrare

                            log.info("Broadcasting HOURLY AGGREGATE for device {} hour: {}", deviceId, previousHour);
                            webSocketHandler.broadcastNewMeasurement(deviceId, wsData);
                            log.info("WebSocket broadcast completed for hour: {}", previousHour);

                        } catch (Exception e) {
                            log.error("Failed to broadcast WebSocket message for {}: {}", deviceId, e.getMessage(), e);
                        }
                    });
        }
    }

    private SensorMeasurement  processValidSensorData(SensorDataDTO sensorData) {
        SensorMeasurement measurement = new SensorMeasurement(
                sensorData.getDeviceId(),
                sensorData.getTimestamp(),
                sensorData.getMeasurementValue()
        );
        SensorMeasurement savedMeasurement = measurementRepository.save(measurement);

        log.info("Saved individual measurement: id={}, deviceId={}, timestamp={}, value={} kWh",
                savedMeasurement.getId(),
                savedMeasurement.getDeviceId(),
                savedMeasurement.getTimestamp(),
                savedMeasurement.getMeasurementValue());

        LocalDateTime currentTimestamp = sensorData.getTimestamp();
        if (currentTimestamp.getMinute() == 0 && currentTimestamp.getSecond() == 0) {
            LocalDateTime previousHour = currentTimestamp.truncatedTo(ChronoUnit.HOURS).minusHours(1);
            createHourlyAggregateForCompletedHour(sensorData.getDeviceId(), previousHour);
        }

        return savedMeasurement;
    }

    private void createHourlyAggregateForCompletedHour(UUID deviceId, LocalDateTime hourTimestamp) {
        LocalDateTime hourStart = hourTimestamp;
        LocalDateTime hourEnd = hourTimestamp.plusHours(1);

        List<SensorMeasurement> measurements = measurementRepository.findByDeviceIdAndTimestampBetween(
                deviceId, hourStart, hourEnd);

        if (measurements.isEmpty()) {
            log.debug("No measurements found for device {} in hour {}", deviceId, hourTimestamp);
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

        log.info("Created hourly aggregate for COMPLETED hour: id={}, deviceId={}, hour={}, total={} kWh",
                saved.getId(), saved.getDeviceId(), saved.getHour(), saved.getTotalConsumption());
    }

    @RabbitListener(queues = RabbitMQConfig.MONITORING_SYNC_QUEUE)
    @Transactional
    public void handleSyncMessage(SyncMessageDTO message) {
        log.info("Monitoring received sync message: {}", message.getEventType());

        try {
            switch (message.getEventType()) {
                case "DEVICE_CREATED":
                    handleDeviceCreated(message.getDeviceId());
                    break;

                case "DEVICE_DELETED":
                    handleDeviceDeleted(message.getDeviceId());
                    break;

                case "USER_CREATED":
                case "USER_ID_ASSIGNED":
                case "USER_CREATE_FAILED":
                case "USER_DELETED":
                    log.debug("Ignoring user event: {}", message.getEventType());
                    break;

                default:
                    log.warn("Unknown event type: {}", message.getEventType());
            }
        } catch (Exception e) {
            log.error("Failed to process sync message: {}", e.getMessage(), e);
            throw e;
        }
    }

    private void handleDeviceCreated(UUID deviceId) {
        try {
            if (deviceCacheRepository.existsById(deviceId)) {
                log.info("Device {} already in cache (idempotent operation)", deviceId);
                return;
            }

            DeviceCache cache = new DeviceCache(deviceId);
            deviceCacheRepository.save(cache);

            log.info("Device {} added to cache", deviceId);

        } catch (Exception e) {
            log.error("Failed to add device {} to cache: {}", deviceId, e.getMessage());
            throw e;
        }
    }

    private void handleDeviceDeleted(UUID deviceId) {
        try {
            deviceCacheRepository.deleteById(deviceId);
            log.info("Device {} removed from cache", deviceId);

            int deletedMeasurements = measurementRepository.deleteByDeviceId(deviceId);
            log.info("Deleted {} individual measurements for device {}", deletedMeasurements, deviceId);

            int deletedAggregates = consumptionRepository.deleteByDeviceId(deviceId);
            log.info("Deleted {} hourly aggregates for device {}", deletedAggregates, deviceId);

        } catch (Exception e) {
            log.error("Failed to handle device deletion for {}: {}", deviceId, e.getMessage());
            throw e;
        }
    }
}