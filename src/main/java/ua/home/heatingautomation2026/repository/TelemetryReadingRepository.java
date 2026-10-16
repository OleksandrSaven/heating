package ua.home.heatingautomation2026.repository;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ua.home.heatingautomation2026.entity.TelemetryReading;

import java.util.List;
import java.util.UUID;

public interface TelemetryReadingRepository
        extends JpaRepository<TelemetryReading, UUID> {

    @EntityGraph(attributePaths = {
            "deviceParameter",
            "deviceParameter.device",
            "deviceParameter.parameterDefinition",
            "sensor"
    })
    List<TelemetryReading>
    findTop100ByDeviceParameter_Device_DeviceIdOrderByReceivedAtDesc(
            String deviceId
    );

    @EntityGraph(attributePaths = {
            "deviceParameter",
            "deviceParameter.device",
            "deviceParameter.parameterDefinition",
            "sensor"
    })
    List<TelemetryReading> findTop100ByOrderByReceivedAtDesc();

    @Modifying(flushAutomatically = true)
    @Query("""
        DELETE FROM TelemetryReading tr
        WHERE tr.deviceParameter.id IN (
            SELECT dp.id
            FROM DeviceParameter dp
            WHERE dp.device.id = :deviceId
        )
        """)
    int deleteAllByDeviceId(@Param("deviceId") UUID deviceId);
}