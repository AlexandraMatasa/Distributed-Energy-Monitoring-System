package com.energy.monitoringservice.repository;

import com.energy.monitoringservice.entity.DeviceCache;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface DeviceCacheRepository extends JpaRepository<DeviceCache, UUID> {
}
