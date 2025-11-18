package com.energy.monitoringservice.controller;

import com.energy.monitoringservice.entity.HourlyEnergyConsumption;
import com.energy.monitoringservice.repository.HourlyConsumptionRepository;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/monitoring")
@CrossOrigin(origins = "*")
public class MonitoringController {

    private final HourlyConsumptionRepository repository;

    public MonitoringController(HourlyConsumptionRepository repository) {
        this.repository = repository;
    }

    @GetMapping("/device/{deviceId}/daily")
    public ResponseEntity<List<HourlyEnergyConsumption>> getDailyConsumption(
            @PathVariable UUID deviceId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

        LocalDateTime startOfDay = date.atStartOfDay();
        LocalDateTime endOfDay = date.atTime(LocalTime.MAX);

        List<HourlyEnergyConsumption> data = repository
                .findByDeviceIdAndHourBetween(deviceId, startOfDay, endOfDay);

        return ResponseEntity.ok(data);
    }
}