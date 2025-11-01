package com.example.devicemanagement.repositories;

import com.example.devicemanagement.entities.DeviceUserAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

public interface DeviceUserAssignmentRepository extends JpaRepository<DeviceUserAssignment, UUID> {

    List<DeviceUserAssignment> findByUserId(UUID userId);

    List<DeviceUserAssignment> findByDeviceId(UUID deviceId);

    @Transactional
    void deleteByUserId(UUID userId);

    @Transactional
    void deleteByDeviceId(UUID deviceId);
}