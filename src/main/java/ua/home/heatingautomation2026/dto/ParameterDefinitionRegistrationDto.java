package ua.home.heatingautomation2026.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import ua.home.heatingautomation2026.enam.ParameterDataType;

public record ParameterDefinitionRegistrationDto(
        @NotBlank(message = "Parameter name must not be empty")
        @Size(max = 100, message = "Parameter code must not exceed 100 characters")
        @Pattern(
                regexp = "^[A-Za-z0-9][A-Za-z0-9._-]*$",
                message = "Parameter code contains invalid characters")
        String code,

        @NotBlank(message = "Parameter name must not be empty")
        @Size(max = 150, message = "Parameter name must not exceed 150 characters")
        String name,

        String description,

        @NotNull(message = "Parameter data type must not be null")
        ParameterDataType parameterDataType,

        @Size(max = 30, message = "Unit must not exceed 30 characters")
        String unit
) {
}
