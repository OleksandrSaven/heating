package ua.home.heatingautomation2026.service;

import ua.home.heatingautomation2026.dto.TelemetryReadingResponse;
import ua.home.heatingautomation2026.dto.TelemetryRequest;

import java.util.List;

public interface TelemetryService {

    int ingest(String deviceId, TelemetryRequest request);

    List<TelemetryReadingResponse> getTelemetryReadings(String deviceId);
}
