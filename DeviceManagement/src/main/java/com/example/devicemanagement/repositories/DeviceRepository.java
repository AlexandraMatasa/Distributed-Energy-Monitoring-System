package com.example.devicemanagement.repositories;

import com.example.devicemanagement.entities.Device;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DeviceRepository extends JpaRepository<Device, UUID> {

    Optional<Device> findByName(String name);

    List<Device> findByMaxConsumptionGreaterThan(Double consumption);

}