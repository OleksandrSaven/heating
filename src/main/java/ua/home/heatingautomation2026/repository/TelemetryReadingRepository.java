package ua.home.heatingautomation2026.repository;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ua.home.heatingautomation2026.entity.TelemetryReading;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TelemetryReadingRepository
        extends JpaRepository<TelemetryReading, UUID> {

    @EntityGraph(attributePaths = {
            "sensor",
            "sensor.device",
            "sensor.parameterDefinition"
    })
    List<TelemetryReading> findTop500BySensor_Device_DeviceIdOrderByReceivedAtDesc(String deviceId);

    @EntityGraph(attributePaths = {
            "sensor",
            "sensor.device",
            "sensor.parameterDefinition"
    })
    List<TelemetryReading> findTop100ByOrderByReceivedAtDesc();

    @EntityGraph(attributePaths = {
            "sensor",
            "sensor.device",
            "sensor.parameterDefinition"
    })
    List<TelemetryReading> findTop500BySensor_SensorIdOrderByMeasuredAtDesc(
            String sensorId
    );

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

    @EntityGraph(attributePaths = {
            "sensor",
            "sensor.parameterDefinition",
            "sensor.device"
    })
    Optional<TelemetryReading>
    findTopBySensor_SensorIdOrderByReceivedAtDesc(String sensorId);

    @Query("""
        SELECT tr
        FROM TelemetryReading tr
        JOIN FETCH tr.sensor s
        JOIN FETCH s.device d
        JOIN FETCH s.parameterDefinition pd
        WHERE tr.numericValue IS NOT NULL
          AND pd.unit IN ('°C', 'C', '℃', '°F', 'F')
          AND tr.receivedAt >= :from
        ORDER BY tr.receivedAt ASC
        """)
    List<TelemetryReading> findTemperatureReadingsSince(
            @Param("from") Instant from
    );
}