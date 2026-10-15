package ua.home.heatingautomation2026.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import tools.jackson.databind.JsonNode;

import java.time.Instant;
import java.util.Map;

public record TelemetryRequest(

        Instant measuredAt,

        @NotEmpty(message = "Telemetry values must not be empty")
        @Size(max = 100, message = "Maximum 100 parameters per request")
        Map<
                @NotBlank(message = "Parameter code must not be empty")
                @Size(max = 100, message = "Parameter code must not exceed 100 characters")
                        String,
                @NotNull(message = "Parameter value must not be null")
                        JsonNode
                > values

) {
}