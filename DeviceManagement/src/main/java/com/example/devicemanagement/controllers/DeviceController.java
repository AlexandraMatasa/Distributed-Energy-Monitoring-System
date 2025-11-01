package com.example.devicemanagement.controllers;

import com.example.devicemanagement.dtos.DeviceAssignmentDTO;
import com.example.devicemanagement.dtos.DeviceDTO;
import com.example.devicemanagement.dtos.DeviceDetailsDTO;
import com.example.devicemanagement.dtos.DeviceWithUserDTO;
import com.example.devicemanagement.services.DeviceService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/device")
@Validated
public class DeviceController {

    private final DeviceService deviceService;

    public DeviceController(DeviceService deviceService) {
        this.deviceService = deviceService;
    }

    @GetMapping
    public ResponseEntity<List<DeviceDTO>> getDevices() {
        return ResponseEntity.ok(deviceService.findDevices());
    }

    @PostMapping
    public ResponseEntity<Void> create(@Valid @RequestBody DeviceDetailsDTO device) {
        UUID id = deviceService.insert(device);
        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(id)
                .toUri();
        return ResponseEntity.created(location).build();
    }

    @GetMapping("/{id}")
    public ResponseEntity<DeviceDetailsDTO> getDevice(@PathVariable UUID id) {
        return ResponseEntity.ok(deviceService.findDeviceById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Void> update(@PathVariable UUID id, @Valid @RequestBody DeviceDetailsDTO device) {
        deviceService.update(id, device);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        deviceService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/user")
    public ResponseEntity<Void> assignDeviceToUser(@RequestBody DeviceAssignmentDTO assignmentDTO) {
        deviceService.assignDeviceToUser(assignmentDTO.getDeviceId(), assignmentDTO.getUserId());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/user/{deviceId}")
    public ResponseEntity<Void> removeDeviceFromUser(@PathVariable UUID deviceId) {
        deviceService.unassignDevice(deviceId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<DeviceWithUserDTO>> getDevicesByUserId(@PathVariable UUID userId) {
        return ResponseEntity.ok(deviceService.findDevicesByUserId(userId));
    }
}