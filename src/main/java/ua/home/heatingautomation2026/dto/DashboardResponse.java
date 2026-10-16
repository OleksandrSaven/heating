package ua.home.heatingautomation2026.dto;

import java.util.List;

public record DashboardResponse(
        long totalDevices,
        long activeDevices,
        List<TelemetryReadingResponse> latestReadings
) {
}