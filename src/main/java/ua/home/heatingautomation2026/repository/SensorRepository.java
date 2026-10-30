package ua.home.heatingautomation2026.repository;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ua.home.heatingautomation2026.entity.Device;
import ua.home.heatingautomation2026.entity.Sensor;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public interface SensorRepository extends JpaRepository<Sensor, UUID> {

    Optional<Sensor> findBySensorId(String sensorId);

    @Query("""
        SELECT s
        FROM Sensor s
        LEFT JOIN FETCH s.parameterDefinition
        WHERE s.sensorId = :sensorId
        """)
    Optional<Sensor> findBySensorIdWithParameterDefinition(
            @Param("sensorId") String sensorId
    );

    boolean existsBySensorId(String sensorId);

    @EntityGraph(attributePaths = {
            "device",
            "parameterDefinition"
    })
    List<Sensor> findAllByOrderBySensorIdAsc();

    @EntityGraph(attributePaths = {
            "device",
            "parameterDefinition"
    })
    List<Sensor> findAllByDevice_DeviceIdOrderBySensorIdAsc(
            String deviceId
    );

    List<Sensor> findAllByDevice_DeviceIdAndEnabledTrueOrderBySensorIdAsc(
            String deviceId
    );

    @EntityGraph(attributePaths = {
            "parameterDefinition",
            "device"
    })
    Optional<Sensor> findWithDetailsBySensorId(String sensorId);

    boolean existsByDeviceAndSensorId(
            Device device,
            String sensorId
    );

    Optional<Sensor> findByDeviceAndSensorId(
            Device device,
            String sensorId
    );

    @Modifying(flushAutomatically = true)
    @Query("""
        DELETE FROM Sensor s
        WHERE s.device.id = :deviceId
        """)
    void deleteAllByDeviceId(@Param("deviceId") UUID deviceId);

    Collection<Sensor> findAllBySensorIdIn(Set<String> sensorIds);

    @Query("""
    SELECT s
    FROM Sensor s
    JOIN FETCH s.device d
    LEFT JOIN FETCH s.parameterDefinition
    WHERE s.sensorId IN :sensorIds
""")
    List<Sensor> findAllWithDetailsBySensorIds(
            @Param("sensorIds") Collection<String> sensorIds);

    List<Sensor> findAllBySensorIdIn(Collection<String> sensorIds);
}