package ua.home.heatingautomation2026.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import ua.home.heatingautomation2026.enam.DeviceType;

public record DeviceRequestRegistrationDto(
        @NotBlank(message = "deviceId must not be empty")
        @Size(max = 100)
        String deviceId,
        @NotBlank(message = "device name must not be empty")
        String deviceName,
        @NotNull(message = "device type must not be null")
        DeviceType deviceType
){
}
