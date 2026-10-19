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
import ua.home.heatingautomation2026.entity.Sensor;
import ua.home.heatingautomation2026.entity.TelemetryReading;
import ua.home.heatingautomation2026.exception.EntityNotFoundException;
import ua.home.heatingautomation2026.exception.EntityValidationException;
import ua.home.heatingautomation2026.mapper.TelemetryReadingMapper;
import ua.home.heatingautomation2026.repository.DeviceRepository;
import ua.home.heatingautomation2026.repository.SensorRepository;
import ua.home.heatingautomation2026.repository.TelemetryReadingRepository;
import ua.home.heatingautomation2026.service.TelemetryService;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class TelemetryServiceImpl implements TelemetryService {
    private final TelemetryReadingRepository telemetryReadingRepository;
    private final DeviceRepository deviceRepository;
    private final TelemetryReadingMapper telemetryReadingMapper;
    private final SensorRepository sensorRepository;

    @Override
    @Transactional
    public int ingest(String deviceId, TelemetryRequest request) {

        // 1. Перевіряємо пристрій
        Device device = deviceRepository.findByDeviceId(deviceId)
                .orElseThrow(() ->
                        new EntityValidationException("Device with id " + deviceId + " not found")
                );

        // 2. Телеметрію можуть надсилати лише активні пристрої
        if (device.getDeviceRegistrationStatus()
                != DeviceRegistrationStatus.ACTIVE) {

            throw new EntityValidationException(
                    "Device " + deviceId
                            + " is not ACTIVE and cannot send telemetry"
            );
        }

        List<TelemetryReading> readings = new ArrayList<>();
        Set<String> processedSensorIds = new HashSet<>();

        // 3. Ключі values тепер є sensorId, а не кодами параметрів
        for (var entry : request.values().entrySet()) {

            String sensorId = entry.getKey().trim();
            JsonNode value = entry.getValue();

            if (sensorId.isBlank()) {
                throw new EntityValidationException(
                        "Sensor ID cannot be empty"
                );
            }

            if (!processedSensorIds.add(sensorId)) {
                throw new EntityValidationException(
                        "Duplicate sensor ID: " + sensorId
                );
            }

            if (value == null || value.isNull()) {
                throw new EntityValidationException(
                        "Value cannot be null for sensor: " + sensorId
                );
            }

            // 4. Знаходимо фізичний датчик
            Sensor sensor = sensorRepository.findBySensorId(sensorId)
                    .orElseThrow(() ->
                            new EntityNotFoundException("Sensor with id " + sensorId + " not found")
                    );

            // 5. Перевіряємо, чи датчик належить цьому ESP32
            if (!sensor.getDevice().getDeviceId().equals(deviceId)) {
                throw new EntityValidationException(
                        "Sensor " + sensorId
                                + " does not belong to device " + deviceId
                );
            }

            // 6. Перевіряємо стан датчика
            if (!sensor.isEnabled()) {
                throw new EntityValidationException(
                        "Sensor " + sensorId + " is disabled"
                );
            }

            // 7. Перевіряємо наявність призначеного параметра
            if (sensor.getParameterDefinition() == null) {
                throw new EntityValidationException(
                        "Sensor " + sensorId
                                + " has no parameter definition"
                );
            }

            // 8. Тип значення визначається через параметр датчика
            ParameterDataType dataType =
                    sensor.getParameterDefinition().getParameterDataType();

            BigDecimal numericValue = null;
            Boolean booleanValue = null;
            String textValue = null;

            switch (dataType) {

                case NUMBER -> {
                    if (!value.isNumber()) {
                        throw new EntityValidationException(
                                "Sensor " + sensorId
                                        + " expects a numeric value"
                        );
                    }

                    numericValue = value.decimalValue();
                }

                case BOOLEAN -> {
                    if (!value.isBoolean()) {
                        throw new EntityValidationException(
                                "Sensor " + sensorId
                                        + " expects a boolean value"
                        );
                    }

                    booleanValue = value.booleanValue();
                }

                case STRING -> {
                    if (!value.isTextual()) {
                        throw new EntityValidationException(
                                "Sensor " + sensorId
                                        + " expects a string value"
                        );
                    }

                    textValue = value.textValue();

                    if (textValue.length() > 2000) {
                        throw new EntityValidationException(
                                "Value is too long for sensor: " + sensorId
                        );
                    }
                }
            }

            // 9. Зберігаємо посилання на фізичний датчик
            readings.add(
                    new TelemetryReading(
                            sensor,
                            numericValue,
                            booleanValue,
                            textValue,
                            request.measuredAt()
                    )
            );
        }

        // Записи зберігаємо лише після перевірки всіх значень
        telemetryReadingRepository.saveAll(readings);

        return readings.size();
    }

    @Override
    public List<TelemetryReadingResponse> getTelemetryReadings(
            String deviceId
    ) {
        var device = findDevice(deviceId);

        return telemetryReadingRepository
                .findTop100BySensor_Device_DeviceIdOrderByReceivedAtDesc(
                        device.getDeviceId()).stream()
                .map(telemetryReadingMapper::toTelemetryReadingResponse)
                .toList();
    }

    private Device findDevice(String deviceId) {
        return deviceRepository.findByDeviceId(deviceId).orElseThrow(
                () -> new EntityNotFoundException("Device with id " + deviceId + " not found"));
    }
}

