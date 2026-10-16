package ua.home.heatingautomation2026.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;
import ua.home.heatingautomation2026.dto.TelemetryReadingResponse;
import ua.home.heatingautomation2026.dto.TelemetryRequest;
import ua.home.heatingautomation2026.enam.DeviceRegistrationStatus;
import ua.home.heatingautomation2026.enam.ParameterDataType;
import ua.home.heatingautomation2026.entity.Device;
import ua.home.heatingautomation2026.entity.DeviceParameter;
import ua.home.heatingautomation2026.entity.TelemetryReading;
import ua.home.heatingautomation2026.exception.EntityNotFoundException;
import ua.home.heatingautomation2026.exception.EntityValidationException;
import ua.home.heatingautomation2026.mapper.TelemetryReadingMapper;
import ua.home.heatingautomation2026.repository.DeviceParameterRepository;
import ua.home.heatingautomation2026.repository.DeviceRepository;
import ua.home.heatingautomation2026.repository.TelemetryReadingRepository;
import ua.home.heatingautomation2026.service.TelemetryService;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class TelemetryServiceImpl implements TelemetryService {
    private final DeviceParameterRepository deviceParameterRepository;
    private final TelemetryReadingRepository telemetryReadingRepository;
    private final DeviceRepository deviceRepository;
    private final TelemetryReadingMapper telemetryReadingMapper;


    @Override
    @Transactional
    public int ingest(String deviceId, TelemetryRequest request) {

        // 1. Перевіряємо, чи існує пристрій
        Device device = findDevice(deviceId);

        // 2. Приймати телеметрію можуть лише активні пристрої
        if (device.getDeviceRegistrationStatus()
                != DeviceRegistrationStatus.ACTIVE) {

            throw new EntityValidationException(
                    "Device " + deviceId
                            + " is not ACTIVE and cannot send telemetry"
            );
        }

        List<TelemetryReading> readings = new ArrayList<>();
        Set<String> processedCodes = new HashSet<>();

        // 3. Перевіряємо кожен параметр із запиту
        for (var entry : request.values().entrySet()) {

            String parameterCode = entry.getKey()
                    .trim()
                    .toLowerCase(Locale.ROOT);

            JsonNode value = entry.getValue();

            // Захищаємося від однакових кодів після нормалізації
            if (!processedCodes.add(parameterCode)) {
                throw new EntityValidationException(
                        "Duplicate parameter code: " + parameterCode
                );
            }

            if (value == null || value.isNull()) {
                throw new EntityValidationException(
                        "Value cannot be null for parameter: "
                                + parameterCode
                );
            }

            // 4. Шукаємо параметр, призначений саме цьому пристрою
            DeviceParameter assignment =
                    deviceParameterRepository
                            .findByDevice_DeviceIdAndParameterDefinition_Code(
                                    deviceId,
                                    parameterCode
                            )
                            .orElseThrow(() ->
                                    new EntityNotFoundException(
                                            "Parameter "
                                                    + parameterCode
                                                    + " is not assigned to device "
                                                    + deviceId
                                    )
                            );

            // 5. Перевіряємо, чи параметр увімкнений
            if (!assignment.isEnabled()) {
                throw new EntityValidationException(
                        "Parameter " + parameterCode
                                + " is disabled for device " + deviceId
                );
            }
            if (assignment.getSensor() != null
                    && !assignment.getSensor().isEnabled()) {

                throw new EntityValidationException(
                        "Sensor "
                                + assignment.getSensor().getSensorId()
                                + " is disabled"
                );
            }

            // 6. Перевіряємо тип і готуємо значення для збереження
            ParameterDataType dataType =
                    assignment.getParameterDefinition().getParameterDataType();

            BigDecimal numericValue = null;
            Boolean booleanValue = null;
            String textValue = null;

            switch (dataType) {

                case NUMBER -> {
                    if (!value.isNumber()) {
                        throw new EntityValidationException(
                                "Parameter " + parameterCode
                                        + " must be a number"
                        );
                    }

                    numericValue = value.decimalValue();
                }

                case BOOLEAN -> {
                    if (!value.isBoolean()) {
                        throw new EntityValidationException(
                                "Parameter " + parameterCode
                                        + " must be a boolean"
                        );
                    }

                    booleanValue = value.booleanValue();
                }

                case STRING -> {
                    if (!value.isString()) {
                        throw new EntityValidationException(
                                "Parameter " + parameterCode
                                        + " must be a string"
                        );
                    }

                    textValue = value.stringValue();

                    if (textValue.length() > 2000) {
                        throw new EntityValidationException(
                                "Value is too long for parameter: "
                                        + parameterCode
                        );
                    }
                }
            }

            // 7. Формуємо запис, але поки не зберігаємо його
            readings.add(
                    new TelemetryReading(
                            assignment,
                            numericValue,
                            booleanValue,
                            textValue,
                            request.measuredAt()
                    )
            );
        }

        // 8. Зберігаємо записи лише після успішної перевірки всіх значень
        telemetryReadingRepository.saveAll(readings);

        return readings.size();
    }

    @Override
    public List<TelemetryReadingResponse> getTelemetryReadings(String deviceId) {
        var device = findDevice(deviceId);

        return telemetryReadingRepository.findTop100ByDeviceParameter_Device_DeviceIdOrderByReceivedAtDesc(device.getDeviceId()).stream()
                .map(telemetryReadingMapper::toTelemetryReadingResponse)
                .toList();
    }

    private Device findDevice(String deviceId) {
        return deviceRepository.findByDeviceId(deviceId).orElseThrow(
                () -> new EntityNotFoundException("Device with id " + deviceId + " not found"));
    }
}

