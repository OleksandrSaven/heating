package ua.home.heatingautomation2026.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;
import ua.home.heatingautomation2026.dto.ParameterDefinitionDto;
import ua.home.heatingautomation2026.dto.ParameterDefinitionRegistrationDto;
import ua.home.heatingautomation2026.entity.ParameterDefinition;

@Mapper(componentModel = "spring", unmappedSourcePolicy = ReportingPolicy.IGNORE)
public interface ParameterDefinitionMapper {

    ParameterDefinition toModel(ParameterDefinitionRegistrationDto requestDto);

    ParameterDefinitionDto toDto(ParameterDefinition requestDto);
}
