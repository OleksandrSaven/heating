package ua.home.heatingautomation2026.dto;


import ua.home.heatingautomation2026.enam.ParameterDataType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record TelemetryReadingResponse(
        UUID id,
        String sensorId,
        String sensorName,
        String deviceId,
        String deviceName,
        String parameterCode,
        String parameterName,
        ParameterDataType dataType,
        String unit,
        BigDecimal numericValue,
        Boolean booleanValue,
        String textValue,
        Instant measuredAt,
        Instant receivedAt
) {
}