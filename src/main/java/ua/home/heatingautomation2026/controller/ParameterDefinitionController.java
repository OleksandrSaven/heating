package ua.home.heatingautomation2026.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ua.home.heatingautomation2026.dto.ParameterDefinitionDto;
import ua.home.heatingautomation2026.dto.ParameterDefinitionRegistrationDto;
import ua.home.heatingautomation2026.mapper.ParameterDefinitionMapper;
import ua.home.heatingautomation2026.service.ParameterDefinitionService;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/parameters")
public class ParameterDefinitionController {

    private final ParameterDefinitionService parameterDefinitionService;
    private final ParameterDefinitionMapper parameterDefinitionMapper;


    @PostMapping
    public ResponseEntity<ParameterDefinitionDto> saveParameterDefinition(@Valid
            @RequestBody ParameterDefinitionRegistrationDto requestDto){
        var parameterDefinition = parameterDefinitionService.createParameterDefinition(requestDto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(parameterDefinitionMapper.toDto(parameterDefinition));

    }

    @GetMapping
    public List<ParameterDefinitionDto> getAllParameterDefinitions(){
        return parameterDefinitionService.getAllParameterDefinitions().stream()
                .map(parameterDefinitionMapper::toDto)
                .toList();
    }
}
