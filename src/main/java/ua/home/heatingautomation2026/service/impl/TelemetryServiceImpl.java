package ua.home.heatingautomation2026.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ua.home.heatingautomation2026.dto.TelemetryRequest;
import ua.home.heatingautomation2026.dto.TelemetryReadingResponse;
import ua.home.heatingautomation2026.enam.DeviceRegistrationStatus;
import ua.home.heatingautomation2026.enam.ParameterDataType;
import ua.home.heatingautomation2026.entity.Device;
import ua.home.heatingautomation2026.entity.Sensor;
import ua.home.heatingautomation2026.entity.TelemetryReading;
import ua.home.heatingautomation2026.mapper.TelemetryReadingMapper;
import ua.home.heatingautomation2026.repository.DeviceRepository;
import ua.home.heatingautomation2026.repository.SensorRepository;
import ua.home.heatingautomation2026.repository.TelemetryReadingRepository;

import tools.jackson.databind.JsonNode;
import ua.home.heatingautomation2026.service.TelemetryService;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TelemetryServiceImpl implements TelemetryService {

    private final TelemetryReadingRepository telemetryReadingRepository;
    private final DeviceRepository deviceRepository;
    private final TelemetryReadingMapper telemetryReadingMapper;
    private final SensorRepository sensorRepository;

    /**
     * Кеш останніх значень датчиків.
     *
     * Ключ — deviceId.
     * Значення — останнє відоме значення кожного датчика цього пристрою.
     */
    private final HashMap<String, List<TelemetryReading>> telemetryReadingMap =
            new HashMap<>();

    /**
     * Обробляє телеметрію від пристрою.
     *
     * У БД зберігаються лише нові або змінені значення.
     *
     * @param deviceId ідентифікатор пристрою
     * @param request отримані показники
     * @return кількість оброблених показників
     */

    private static final BigDecimal TEMPERATURE_THRESHOLD =
            new BigDecimal("0.5");

    @Override
    @Transactional
    public synchronized int ingest(String deviceId, TelemetryRequest request) {

        Device device = deviceRepository.findByDeviceId(deviceId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Device not found: " + deviceId
                        )
                );

        if (device.getDeviceRegistrationStatus()!= DeviceRegistrationStatus.ACTIVE) {
            throw new IllegalStateException(
                    "Device is not active: " + deviceId
            );
        }

        if (request == null || request.values() == null
                || request.values().isEmpty()) {
            return 0;
        }

        /*
         * Отримуємо попередні значення з кешу.
         * Якщо пристрій ще не надсилав телеметрію, список буде порожнім.
         */
        List<TelemetryReading> previousReadings =
                telemetryReadingMap.getOrDefault(deviceId, List.of());

        Map<String, TelemetryReading> previousBySensor = new HashMap<>();

        for (TelemetryReading reading : previousReadings) {
            previousBySensor.put(
                    reading.getSensor().getSensorId(),
                    reading
            );
        }

        /*
         * readings — усі показники з поточного повідомлення.
         * changedReadings — лише нові або змінені показники для БД.
         */
        List<TelemetryReading> readings = new ArrayList<>();
        List<TelemetryReading> changedReadings = new ArrayList<>();

        Set<String> processedSensorIds = new HashSet<>();

        for (Map.Entry<String, JsonNode> entry : request.values().entrySet()) {

            String sensorId = entry.getKey();
            JsonNode value = entry.getValue();

            // Перевіряємо ідентифікатор датчика.
            if (sensorId == null || sensorId.isBlank()) {
                throw new IllegalArgumentException(
                        "Sensor ID must not be empty"
                );
            }

            // Не дозволяємо двічі обробляти один датчик у межах повідомлення.
            if (!processedSensorIds.add(sensorId)) {
                throw new IllegalArgumentException(
                        "Duplicate sensor ID: " + sensorId
                );
            }

            if (value == null || value.isNull()) {
                throw new IllegalArgumentException(
                        "Telemetry value must not be null for sensor: "
                                + sensorId
                );
            }

            // Знаходимо датчик.
            Sensor sensor = sensorRepository.findBySensorId(sensorId)
                    .orElseThrow(() ->
                            new IllegalArgumentException(
                                    "Sensor not found: " + sensorId
                            )
                    );

            // Перевіряємо, що датчик належить пристрою, який надіслав дані.
            if (!sensor.getDevice().getDeviceId().equals(deviceId)) {
                throw new IllegalArgumentException(
                        "Sensor " + sensorId
                                + " does not belong to device " + deviceId
                );
            }

            /*
             * Визначаємо тип параметра.
             * Передбачається, що датчик має пов'язане визначення параметра.
             */
            if (sensor.getParameterDefinition() == null) {
                throw new IllegalStateException(
                        "Parameter definition is missing for sensor: "
                                + sensorId
                );
            }

            ParameterDataType dataType =
                    sensor.getParameterDefinition().getParameterDataType();

            BigDecimal numericValue = null;
            Boolean booleanValue = null;
            String textValue = null;

            /*
             * Перетворюємо JSON-значення відповідно до типу параметра.
             */
            switch (dataType) {

                case NUMBER -> {
                    if (!value.isNumber()) {
                        throw new IllegalArgumentException(
                                "Expected numeric value for sensor: "
                                        + sensorId
                        );
                    }

                    numericValue = value.decimalValue();
                }

                case BOOLEAN -> {
                    if (!value.isBoolean()) {
                        throw new IllegalArgumentException(
                                "Expected boolean value for sensor: "
                                        + sensorId
                        );
                    }

                    booleanValue = value.booleanValue();
                }

                case STRING -> {
                    if (!value.isTextual()) {
                        throw new IllegalArgumentException(
                                "Expected string value for sensor: "
                                        + sensorId
                        );
                    }

                    textValue = value.textValue();
                }

                default -> throw new IllegalArgumentException(
                        "Unsupported parameter data type: " + dataType
                );
            }

            /*
             * Створюємо об'єкт поточного показника.
             */
            TelemetryReading currentReading = new TelemetryReading(
                    sensor,
                    numericValue,
                    booleanValue,
                    textValue,
                    request.measuredAt()
            );

            readings.add(currentReading);

            /*
             * Знаходимо попереднє значення цього датчика.
             */
            var previousReading = telemetryReadingRepository.findTopBySensor_SensorIdOrderByReceivedAtDesc(sensorId)
                    .orElse(previousBySensor.get(sensorId));

            /*
             * У БД потрібно записати показник, якщо:
             * 1. Це перше отримане значення датчика.
             * 2. Значення відрізняється від попереднього.
             */
            if (previousReading == null
                    || hasValueChanged(previousReading, currentReading)) {

                changedReadings.add(currentReading);
            }
        }

        /*
         * Зберігаємо в БД лише змінені показники.
         */
        if (!changedReadings.isEmpty()) {
            telemetryReadingRepository.saveAll(changedReadings);
        }

        /*
         * Оновлюємо кеш.
         *
         * Важливо: якщо повідомлення містить лише частину датчиків,
         * попередні значення решти датчиків залишаються в кеші.
         */
        Map<String, TelemetryReading> latestBySensor =
                new LinkedHashMap<>(previousBySensor);

        for (TelemetryReading reading : readings) {
            latestBySensor.put(
                    reading.getSensor().getSensorId(),
                    reading
            );
        }

        telemetryReadingMap.put(
                device.getDeviceId(),
                new ArrayList<>(latestBySensor.values())
        );

        return readings.size();
    }

    @Override
    public synchronized List<TelemetryReadingResponse> getTelemetryReadings(
            String deviceId
    ) {
        Device device = deviceRepository.findByDeviceId(deviceId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Device not found: " + deviceId
                        )
                );

        List<TelemetryReading> readings =
                telemetryReadingMap.getOrDefault(
                        device.getDeviceId(),
                        List.of()
                );

        return readings.stream()
                .map(telemetryReadingMapper::toTelemetryReadingResponse)
                .collect(Collectors.toList());
    }

    /**
     * Повертає останні значення датчиків усіх пристроїв з кешу.
     */
    @Override
    public synchronized List<TelemetryReadingResponse> getTelemetryReadings() {
        return telemetryReadingMap.values()
                .stream()
                .flatMap(List::stream)
                .map(telemetryReadingMapper::toTelemetryReadingResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<TelemetryReadingResponse> getSensorTelemetryReadings(String sensorId) {
        if (sensorId == null || sensorId.isBlank()) {
            throw new IllegalArgumentException(
                    "Sensor ID must not be empty"
            );
        }

        return telemetryReadingRepository
                .findTop500BySensor_SensorIdOrderByMeasuredAtDesc(sensorId)
                .stream()
                .map(telemetryReadingMapper::toTelemetryReadingResponse)
                .toList();
    }

    private boolean hasValueChanged(
            TelemetryReading previous,
            TelemetryReading current
    ) {
        BigDecimal previousNumeric = previous.getNumericValue();
        BigDecimal currentNumeric = current.getNumericValue();

        if (previousNumeric == null || currentNumeric == null) {
            if (!Objects.equals(previousNumeric, currentNumeric)) {
                return true;
            }
        } else {
            String unit = current.getSensor()
                    .getParameterDefinition()
                    .getUnit();

            if (isTemperatureUnit(unit)) {
                BigDecimal difference = previousNumeric
                        .subtract(currentNumeric)
                        .abs();

                return difference.compareTo(TEMPERATURE_THRESHOLD) >= 0;
            }

            if (previousNumeric.compareTo(currentNumeric) != 0) {
                return true;
            }
        }

        if (!Objects.equals(
                previous.getBooleanValue(),
                current.getBooleanValue()
        )) {
            return true;
        }

        return !Objects.equals(
                previous.getTextValue(),
                current.getTextValue()
        );
    }

    private boolean isTemperatureUnit(String unit) {
        if (unit == null) {
            return false;
        }

        return switch (unit.trim().toUpperCase()) {
            case "°C", "C", "℃", "°F", "F" -> true;
            default -> false;
        };
    }
}