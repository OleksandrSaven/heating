package ua.home.heatingautomation2026.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.stereotype.Service;
import ua.home.heatingautomation2026.dto.ParameterDefinitionRegistrationDto;
import ua.home.heatingautomation2026.enam.ParameterDataType;
import ua.home.heatingautomation2026.entity.ParameterDefinition;
import ua.home.heatingautomation2026.exception.EntityNotFoundException;
import ua.home.heatingautomation2026.exception.EntityValidationException;
import ua.home.heatingautomation2026.mapper.ParameterDefinitionMapper;
import ua.home.heatingautomation2026.repository.ParameterDefinitionRepository;
import ua.home.heatingautomation2026.service.ParameterDefinitionService;

import java.util.List;
import java.util.Locale;

@RequiredArgsConstructor
@Setter
@Service
public class ParameterDefinitionServiceImpl implements ParameterDefinitionService {
    private final ParameterDefinitionRepository parameterDefinitionRepository;
    private final ParameterDefinitionMapper parameterDefinitionMapper;

    @Override
    public ParameterDefinition createParameterDefinition(ParameterDefinitionRegistrationDto registrationDto) {
       var code = normalizeCode(registrationDto.code());
       if (parameterDefinitionRepository.findByCode(code).isPresent()) {
           throw new EntityValidationException("Code already exists");
       }

        return parameterDefinitionRepository.save(parameterDefinitionMapper.toModel(registrationDto));
    }


    @Override
    public List<ParameterDefinition> getAllParameterDefinitions() {
        return parameterDefinitionRepository.findAll();
    }

    @Override
    public ParameterDefinition getParameterDefinitionByCode(String code) {
        return parameterDefinitionRepository.findByCode(code).orElseThrow(
                () -> new EntityNotFoundException("Cannot find parameter with code " + code));
    }

    @Override
    public List<ParameterDefinition> findAllByParameterDataType(ParameterDataType parameterDataType) {
        return parameterDefinitionRepository.findAllByParameterDataType(parameterDataType);
    }

    private String normalizeCode(String code) {
        return code.trim().toLowerCase(Locale.ROOT);
    }
}
