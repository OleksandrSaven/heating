package ua.home.heatingautomation2026.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SensorRegistrationDto(

        @NotBlank(message = "sensorId must not be empty")
        @Size(max = 100)
        String sensorId,

        @NotBlank(message = "sensorName must not be empty")
        @Size(max = 150)
        String sensorName,

        @NotBlank(message = "sensorType must not be empty")
        @Size(max = 50)
        String sensorType,

        @NotBlank(message = "parameterCode must not be empty")
        @Size(max = 100)
        String parameterCode

) {
}