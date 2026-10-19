package ua.home.heatingautomation2026.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ua.home.heatingautomation2026.dto.DeviceRequestRegistrationDto;
import ua.home.heatingautomation2026.dto.DeviceResponse;
import ua.home.heatingautomation2026.enam.DeviceRegistrationStatus;
import ua.home.heatingautomation2026.mapper.DeviceMapper;
import ua.home.heatingautomation2026.service.DeviceService;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/devices")
public class DeviceController {
    private final DeviceService deviceService;
    private final DeviceMapper deviceMapper;

    @GetMapping("/{deviceId}")
    public ResponseEntity<DeviceResponse> getDevice(
            @PathVariable String deviceId
    ) {
        var device = deviceService.findDevice(deviceId);
        return ResponseEntity.ok(deviceMapper.toResponse(device));
    }

    @GetMapping
    public List<DeviceResponse> getDevices() {

        return deviceService.findAllDevices().stream()
                .map(deviceMapper::toResponse)
                .toList();
    }

    @PostMapping
    public ResponseEntity<DeviceResponse> registerDevice(@Valid @RequestBody DeviceRequestRegistrationDto registration) {
        var device = deviceService.registerDevice(registration);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(deviceMapper.toResponse(device));
    }

    @GetMapping("/pending")
    public List<DeviceResponse> getPendingDevices() {

        return deviceService.findAllDevicesByDeviceRegistrationStatus(DeviceRegistrationStatus.PENDING).stream()
                .map(deviceMapper::toResponse)
                .toList();
    }

    @PatchMapping("/{id}/approve")
    public ResponseEntity<DeviceResponse> approveDevice(@PathVariable UUID id) {
        var device = deviceService.approveDevice(id);

        return ResponseEntity.ok(deviceMapper.toResponse(device));
    }

    @PatchMapping("/{id}/enable")
    public ResponseEntity<DeviceResponse> enableDevice(@PathVariable UUID id) {
        var device = deviceService.enableDevice(id);
        return ResponseEntity.ok(deviceMapper.toResponse(device));
    }

    @PatchMapping("/{id}/disable")
    public ResponseEntity<DeviceResponse> disableDevice(@PathVariable UUID id) {
        var device = deviceService.disableDevice(id);
        return ResponseEntity.ok(deviceMapper.toResponse(device));
    }

    @PostMapping("/{deviceId}/heartbeat")
    public ResponseEntity<DeviceResponse> heartbeatDevice(@PathVariable String deviceId) {
        var device = deviceService.heartBeat(deviceId);
        return ResponseEntity.ok(deviceMapper.toResponse(device));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteDevice(
            @PathVariable UUID id) {
        deviceService.deleteDevice(id);

        return ResponseEntity.noContent().build();
    }
}
