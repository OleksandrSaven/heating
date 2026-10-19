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
            "sensor",
            "sensor.device",
            "sensor.parameterDefinition"
    })
    List<TelemetryReading> findTop100BySensor_Device_DeviceIdOrderByReceivedAtDesc(String deviceId);

    @EntityGraph(attributePaths = {
            "sensor",
            "sensor.device",
            "sensor.parameterDefinition"
    })
    List<TelemetryReading> findTop100ByOrderByReceivedAtDesc();

    @Modifying(
            flushAutomatically = true,
            clearAutomatically = true
    )
    @Query("""
            DELETE FROM TelemetryReading tr
            WHERE tr.sensor.id IN (
                SELECT s.id
                FROM Sensor s
                WHERE s.device.id = :deviceId
            )
            """)
    int deleteAllByDeviceId(@Param("deviceId") UUID deviceId);
}