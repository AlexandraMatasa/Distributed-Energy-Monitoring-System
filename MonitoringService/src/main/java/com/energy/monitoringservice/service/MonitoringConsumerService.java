package com.energy.monitoringservice.service;

import com.energy.monitoringservice.config.RabbitMQConfig;
import com.energy.monitoringservice.dto.SensorDataDTO;
import com.energy.monitoringservice.dto.SyncMessageDTO;
import com.energy.monitoringservice.entity.DeviceCache;
import com.energy.monitoringservice.entity.HourlyEnergyConsumption;
import com.energy.monitoringservice.repository.DeviceCacheRepository;
import com.energy.monitoringservice.repository.HourlyConsumptionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Service
public class MonitoringConsumerService {

    private static final Logger log = LoggerFactory.getLogger(MonitoringConsumerService.class);

    private final HourlyConsumptionRepository consumptionRepository;
    private final DeviceCacheRepository deviceCacheRepository;

    @Autowired
    public MonitoringConsumerService(HourlyConsumptionRepository consumptionRepository,
                                     DeviceCacheRepository deviceCacheRepository) {
        this.consumptionRepository = consumptionRepository;
        this.deviceCacheRepository = deviceCacheRepository;
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

        processValidSensorData(sensorData);
    }

    private void processValidSensorData(SensorDataDTO sensorData) {
        LocalDateTime hourTimestamp = sensorData.getTimestamp()
                .truncatedTo(ChronoUnit.HOURS);

        HourlyEnergyConsumption hourlyData = consumptionRepository
                .findByDeviceIdAndHour(sensorData.getDeviceId(), hourTimestamp)
                .orElse(null);

        if (hourlyData == null) {
            hourlyData = new HourlyEnergyConsumption();
            hourlyData.setDeviceId(sensorData.getDeviceId());
            hourlyData.setHour(hourTimestamp);
            hourlyData.setTotalConsumption(sensorData.getMeasurementValue());
            hourlyData.setCreatedAt(LocalDateTime.now());

            log.debug("Creating new hourly record for device {} at hour {}",
                    sensorData.getDeviceId(), hourTimestamp);
        } else {
            double newTotal = hourlyData.getTotalConsumption() + sensorData.getMeasurementValue();
            hourlyData.setTotalConsumption(newTotal);

            log.debug("Updating hourly record for device {} at hour {}. New total: {} kWh",
                    sensorData.getDeviceId(), hourTimestamp, newTotal);
        }

        HourlyEnergyConsumption saved = consumptionRepository.save(hourlyData);

        log.info("Saved hourly consumption: id={}, deviceId={}, hour={}, total={} kWh",
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

            int deletedRows = consumptionRepository.deleteByDeviceId(deviceId);
            log.info("Deleted {} consumption records for device {}", deletedRows, deviceId);

        } catch (Exception e) {
            log.error("Failed to handle device deletion for {}: {}", deviceId, e.getMessage());
            throw e;
        }
    }
}