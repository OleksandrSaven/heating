package ua.home.heatingautomation2026.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ua.home.heatingautomation2026.dto.TelemetryIngestResponse;
import ua.home.heatingautomation2026.dto.TelemetryReadingResponse;
import ua.home.heatingautomation2026.dto.TelemetryRequest;
import ua.home.heatingautomation2026.service.TelemetryService;

import java.util.List;

@RestController
@RequestMapping("/api/devices/{deviceId}/telemetry")
@RequiredArgsConstructor
public class TelemetryController {

    private final TelemetryService telemetryService;

    @PostMapping
    public ResponseEntity<TelemetryIngestResponse> ingestTelemetry(@PathVariable String deviceId,
                                                                   @RequestBody TelemetryRequest telemetryRequest) {
        var ingest = telemetryService.ingest(deviceId, telemetryRequest);

        return ResponseEntity.ok(new TelemetryIngestResponse(deviceId, ingest));
    }

    @GetMapping
    public ResponseEntity<List<TelemetryReadingResponse>> getTelemetryReadings(@PathVariable String deviceId) {
        return ResponseEntity.ok(telemetryService.getTelemetryReadings(deviceId));
    }
}
