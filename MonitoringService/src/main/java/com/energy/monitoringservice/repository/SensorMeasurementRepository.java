package com.energy.monitoringservice.repository;

import com.energy.monitoringservice.entity.SensorMeasurement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface SensorMeasurementRepository extends JpaRepository<SensorMeasurement, Long> {

    List<SensorMeasurement> findByDeviceIdAndTimestampBetween(
            UUID deviceId,
            LocalDateTime start,
            LocalDateTime end
    );

    List<SensorMeasurement> findByDeviceIdOrderByTimestampAsc(UUID deviceId);

    @Modifying
    @Query("DELETE FROM SensorMeasurement s WHERE s.deviceId = :deviceId")
    int deleteByDeviceId(@Param("deviceId") UUID deviceId);

    long countByDeviceId(UUID deviceId);
}