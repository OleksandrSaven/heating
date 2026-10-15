package ua.home.heatingautomation2026.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ua.home.heatingautomation2026.enam.ParameterDataType;
import ua.home.heatingautomation2026.entity.ParameterDefinition;

import java.util.List;
import java.util.Optional;

public interface ParameterDefinitionRepository extends JpaRepository<ParameterDefinition, Long> {

    Optional<ParameterDefinition> findByCode(String code);

    List<ParameterDefinition> findAllByParameterDataType(ParameterDataType parameterDataType);
}
