package ua.home.heatingautomation2026.service;

import ua.home.heatingautomation2026.dto.ParameterDefinitionRegistrationDto;
import ua.home.heatingautomation2026.enam.ParameterDataType;
import ua.home.heatingautomation2026.entity.ParameterDefinition;

import java.util.List;

public interface ParameterDefinitionService {

    ParameterDefinition createParameterDefinition(ParameterDefinitionRegistrationDto registrationDto);

    List<ParameterDefinition> getAllParameterDefinitions();

    ParameterDefinition getParameterDefinitionByCode(String code);

    List<ParameterDefinition> findAllByParameterDataType(ParameterDataType parameterDataType);


}
