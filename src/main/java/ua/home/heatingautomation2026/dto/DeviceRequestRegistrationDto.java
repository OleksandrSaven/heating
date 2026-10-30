package ua.home.heatingautomation2026.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import ua.home.heatingautomation2026.enam.DeviceType;

import java.util.List;

public record DeviceRequestRegistrationDto(

        @NotBlank(message = "deviceId must not be empty")
        @Size(max = 100)
        String deviceId,

        @NotBlank(message = "device name must not be empty")
        @Size(max = 100)
        String deviceName,

        @NotNull(message = "device type must not be null")
        DeviceType deviceType,

        @NotNull(message = "sensors must not be null")
        @Size(max = 50, message = "Maximum 50 sensors allowed")
        List<@Valid SensorRegistrationDto> sensors

) {
}