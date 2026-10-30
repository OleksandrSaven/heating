/*
package ua.home.heatingautomation2026.repository;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ua.home.heatingautomation2026.entity.DeviceParameter;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DeviceParameterRepository extends JpaRepository<DeviceParameter, UUID> {

    @EntityGraph(attributePaths = {
            "device",
            "parameterDefinition",
            "sensor"
    })    List<DeviceParameter> findAllByDevice_DeviceId(String deviceId);

    boolean existsByDevice_IdAndParameterDefinition_Id(UUID deviceId, Long parameterDefinitionId);

    @EntityGraph(attributePaths = {
            "device",
            "parameterDefinition",
            "sensor"
    })
    Optional<DeviceParameter> findByDevice_DeviceIdAndParameterDefinition_Code(
            String deviceId,
            String parameterCode
    );

    boolean existsBySensor_Id(UUID sensorId);

    @Modifying(flushAutomatically = true)
    @Query("""
        DELETE FROM DeviceParameter dp
        WHERE dp.device.id = :deviceId
        """)
    int deleteAllByDeviceId(@Param("deviceId") UUID deviceId);
}
*/
