package ua.home.heatingautomation2026.repository;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import ua.home.heatingautomation2026.entity.DeviceParameter;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DeviceParameterRepository extends JpaRepository<DeviceParameter, UUID> {

    @EntityGraph(attributePaths = {"device", "parameterDefinition"})
    List<DeviceParameter> findAllByDevice_DeviceId(String deviceId);

    boolean existsByDevice_IdAndParameterDefinition_Id(UUID deviceId, Long parameterDefinitionId);

    @EntityGraph(attributePaths = {"device", "parameterDefinition"})
    Optional<DeviceParameter> findByDevice_DeviceIdAndParameterDefinition_Code(
            String deviceId,
            String parameterCode
    );
}
