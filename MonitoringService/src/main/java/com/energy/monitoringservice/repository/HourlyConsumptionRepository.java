package com.energy.monitoringservice.repository;

import com.energy.monitoringservice.entity.HourlyEnergyConsumption;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface HourlyConsumptionRepository extends JpaRepository<HourlyEnergyConsumption, Long> {

    Optional<HourlyEnergyConsumption> findByDeviceIdAndHour(UUID deviceId, LocalDateTime hour);

    List<HourlyEnergyConsumption> findByDeviceIdAndHourBetween(
            UUID deviceId,
            LocalDateTime start,
            LocalDateTime end
    );

    @Modifying
    @Query("DELETE FROM HourlyEnergyConsumption h WHERE h.deviceId = :deviceId")
    int deleteByDeviceId(@Param("deviceId") UUID deviceId);
}