package ua.home.heatingautomation2026.repository;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import ua.home.heatingautomation2026.entity.TelemetryReading;

import java.util.List;
import java.util.UUID;

public interface TelemetryReadingRepository
        extends JpaRepository<TelemetryReading, UUID> {

    @EntityGraph(attributePaths = {
            "deviceParameter",
            "deviceParameter.device",
            "deviceParameter.parameterDefinition"
    })
    List<TelemetryReading>
    findTop100ByDeviceParameter_Device_DeviceIdOrderByReceivedAtDesc(
            String deviceId
    );
}