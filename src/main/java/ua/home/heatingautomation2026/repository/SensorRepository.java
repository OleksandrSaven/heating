package ua.home.heatingautomation2026.repository;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import ua.home.heatingautomation2026.entity.Sensor;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SensorRepository extends JpaRepository<Sensor, UUID> {

    Optional<Sensor> findBySensorId(String sensorId);

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
}