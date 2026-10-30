/*
package ua.home.heatingautomation2026.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ua.home.heatingautomation2026.dto.DeviceParameterResponse;
import ua.home.heatingautomation2026.entity.DeviceParameter;
import ua.home.heatingautomation2026.mapper.DeviceParameterMapper;
import ua.home.heatingautomation2026.service.DeviceParameterService;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/devices/{deviceId}/parameters")
public class DeviceParameterController {

    private final DeviceParameterService deviceParameterService;
    private final DeviceParameterMapper deviceParameterMapper;


    @PostMapping("/{parameterCode}")
    public ResponseEntity<DeviceParameterResponse> assignParameter(
            @PathVariable String deviceId,
            @PathVariable String parameterCode
    ) {
        DeviceParameter deviceParameter =
                deviceParameterService.assignParameter(
                        deviceId,
                        parameterCode
                );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(deviceParameterMapper.toResponse(deviceParameter));
    }

    @GetMapping
    public List<DeviceParameterResponse> getDeviceParameters(
            @PathVariable String deviceId
    ) {
        List<DeviceParameter> parameters =
                deviceParameterService.getDeviceParameters(deviceId);

        return deviceParameterMapper.toResponseList(parameters);
    }

    @PatchMapping("/{parameterCode}/enable")
    public ResponseEntity<DeviceParameterResponse> enableParameter(
            @PathVariable String deviceId, @PathVariable String parameterCode) {
        var deviceParameter = deviceParameterService.setParameterStatus(deviceId, parameterCode, true);

        return ResponseEntity.ok(deviceParameterMapper.toResponse(deviceParameter));
    }

    @PatchMapping("/{parameterCode}/disable")
    public ResponseEntity<DeviceParameterResponse> disableParameter(
            @PathVariable String deviceId, @PathVariable String parameterCode) {
        var deviceParameter = deviceParameterService.setParameterStatus(deviceId, parameterCode, false);

        return ResponseEntity.ok(deviceParameterMapper.toResponse(deviceParameter));
    }
}*/
