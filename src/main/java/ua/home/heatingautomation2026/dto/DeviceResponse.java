package ua.home.heatingautomation2026.dto;

import ua.home.heatingautomation2026.enam.DeviceRegistrationStatus;
import ua.home.heatingautomation2026.enam.DeviceType;

import java.time.Instant;
import java.util.UUID;

public record DeviceResponse(
        UUID id,
        String deviceId,
        String deviceName,
        DeviceType deviceType,
        DeviceRegistrationStatus deviceRegistrationStatus,
        Instant lastHeartbeat
) {
}
