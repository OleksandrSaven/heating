package ua.home.heatingautomation2026.dto;

import ua.home.heatingautomation2026.enam.ParameterDataType;

import java.time.Instant;
import java.util.UUID;

public record DeviceParameterResponse(
        UUID id,
        String deviceId,
        String deviceName,
        Long parameterId,
        String parameterCode,
        String parameterName,
        ParameterDataType dataType,
        String unit,
        boolean enabled,
        Instant createdAt,
        String sensorId,
        String sensorName,
        String sensorType
) {
}
