package com.energy.monitoringservice.controller;

import com.energy.monitoringservice.entity.HourlyEnergyConsumption;
import com.energy.monitoringservice.entity.SensorMeasurement;
import com.energy.monitoringservice.repository.HourlyConsumptionRepository;
import com.energy.monitoringservice.repository.SensorMeasurementRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/monitoring")
@CrossOrigin(origins = "*")
public class MonitoringController {

    private static final Logger log = LoggerFactory.getLogger(MonitoringController.class);

    private final HourlyConsumptionRepository consumptionRepository;
    private final SensorMeasurementRepository measurementRepository;

    public MonitoringController(HourlyConsumptionRepository consumptionRepository,
                                SensorMeasurementRepository measurementRepository) {
        this.consumptionRepository = consumptionRepository;
        this.measurementRepository = measurementRepository;
    }

    @GetMapping("/device/{deviceId}/daily")
    public ResponseEntity<List<HourlyEnergyConsumption>> getDailyConsumption(
            @PathVariable UUID deviceId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

        LocalDateTime startOfDay = date.atStartOfDay();
        LocalDateTime endOfDay = date.atTime(LocalTime.MAX);

        List<HourlyEnergyConsumption> data = consumptionRepository
                .findByDeviceIdAndHourBetween(deviceId, startOfDay, endOfDay);

        log.info("GET /device/{}/daily?date={} - Returned {} hourly records",
                deviceId, date, data.size());

        return ResponseEntity.ok(data);
    }

    @GetMapping("/device/{deviceId}/measurements")
    public ResponseEntity<List<SensorMeasurement>> getIndividualMeasurements(
            @PathVariable UUID deviceId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

        LocalDateTime startOfDay = date.atStartOfDay();
        LocalDateTime endOfDay = date.atTime(LocalTime.MAX);

        List<SensorMeasurement> data = measurementRepository
                .findByDeviceIdAndTimestampBetween(deviceId, startOfDay, endOfDay);

        log.info("GET /device/{}/measurements?date={} - Returned {} individual measurements",
                deviceId, date, data.size());

        return ResponseEntity.ok(data);
    }

    @GetMapping("/device/{deviceId}/stats")
    public ResponseEntity<Map<String, Object>> getDeviceStats(@PathVariable UUID deviceId) {

        long totalMeasurements = measurementRepository.countByDeviceId(deviceId);
        long totalHours = consumptionRepository.count();

        Map<String, Object> stats = new HashMap<>();
        stats.put("deviceId", deviceId);
        stats.put("totalMeasurements", totalMeasurements);
        stats.put("totalHourlyRecords", totalHours);

        log.info("GET /device/{}/stats - Total measurements: {}, Total hours: {}",
                deviceId, totalMeasurements, totalHours);

        return ResponseEntity.ok(stats);
    }

    @GetMapping("/device/{deviceId}/all-measurements")
    public ResponseEntity<List<SensorMeasurement>> getAllMeasurements(@PathVariable UUID deviceId) {

        List<SensorMeasurement> data = measurementRepository
                .findByDeviceIdOrderByTimestampAsc(deviceId);

        log.info("GET /device/{}/all-measurements - Returned {} measurements",
                deviceId, data.size());

        return ResponseEntity.ok(data);
    }
}