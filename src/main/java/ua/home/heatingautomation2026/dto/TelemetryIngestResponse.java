package ua.home.heatingautomation2026.dto;

public record TelemetryIngestResponse(
        String deviceId,
        int acceptedReadings

) {
}
