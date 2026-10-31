package ua.home.heatingautomation2026.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ua.home.heatingautomation2026.dto.TelemetryRequest;
import ua.home.heatingautomation2026.dto.TelemetryReadingResponse;
import ua.home.heatingautomation2026.enam.DeviceConnectivityStatus;
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
import ua.home.heatingautomation2026.service.DeviceConnectivityService;
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
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TelemetryServiceImpl implements TelemetryService {

    private final TelemetryReadingRepository telemetryReadingRepository;
    private final DeviceRepository deviceRepository;
    private final TelemetryReadingMapper telemetryReadingMapper;
    private final SensorRepository sensorRepository;
    private final DeviceConnectivityService deviceConnectivityService;

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
            new BigDecimal("4");

    @Override
    @Transactional
    public synchronized int ingest(String deviceId, TelemetryRequest request) {

        // 1. Перевіряємо пристрій
        Device device = deviceRepository.findByDeviceId(deviceId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Device not found: " + deviceId
                        )
                );

        if (device.getDeviceRegistrationStatus() != DeviceRegistrationStatus.ACTIVE) {
            throw new IllegalStateException(
                    "Device is not active: " + deviceId
            );
        }

        if (request == null || request.values() == null
                || request.values().isEmpty()) {
            return 0;
        }

        // 2. Перевіряємо ID датчиків до звернення до БД
        Set<String> sensorIds = request.values().keySet();

        Set<String> processedSensorIds = new HashSet<>();

        for (Map.Entry<String, JsonNode> entry : request.values().entrySet()) {

            String sensorId = entry.getKey();
            JsonNode value = entry.getValue();

            if (sensorId == null || sensorId.isBlank()) {
                throw new IllegalArgumentException(
                        "Sensor ID must not be empty"
                );
            }

            if (!processedSensorIds.add(sensorId)) {
                throw new IllegalArgumentException(
                        "Duplicate sensor ID: " + sensorId
                );
            }

            /*if (value == null || value.isNull()) {
                throw new IllegalArgumentException(
                        "Telemetry value must not be null for sensor: "
                                + sensorId
                );*/
           // }
        }

        // 3. Одним запитом отримуємо всі датчики з пов'язаними Entity
        Map<String, Sensor> sensorsMap = sensorRepository
                .findAllWithDetailsBySensorIds(sensorIds)
                .stream()
                .collect(Collectors.toMap(
                        Sensor::getSensorId,
                        Function.identity()
                ));

        // 4. Перевіряємо наявність усіх датчиків
        for (String sensorId : sensorIds) {

            Sensor sensor = sensorsMap.get(sensorId);

            if (sensor == null) {
                throw new IllegalArgumentException(
                        "Sensor not found: " + sensorId
                );
            }

            if (!sensor.getDevice().getDeviceId().equals(deviceId)) {
                throw new IllegalArgumentException(
                        "Sensor " + sensorId
                                + " does not belong to device " + deviceId
                );
            }
        }

        // 5. Завантажуємо останні збережені показники одним запитом
        Map<String, TelemetryReading> previousBySensor =
                telemetryReadingRepository
                        .findLatestBySensorIds(sensorIds)
                        .stream()
                        .collect(Collectors.toMap(
                                reading -> reading.getSensor().getSensorId(),
                                Function.identity()
                        ));

        // 6. Отримуємо поточний кеш
        List<TelemetryReading> cachedReadings =
                telemetryReadingMap.getOrDefault(
                        deviceId,
                        List.of()
                );

        Map<String, TelemetryReading> latestBySensor =
                new LinkedHashMap<>();

        for (TelemetryReading reading : cachedReadings) {
            latestBySensor.put(
                    reading.getSensor().getSensorId(),
                    reading
            );
        }

        // 7. Якщо кеш порожній, відновлюємо його з БД
        if (cachedReadings.isEmpty()) {

            List<TelemetryReading> allLatestReadings =
                    telemetryReadingRepository
                            .findLatestByDeviceId(deviceId);

            for (TelemetryReading reading : allLatestReadings) {

                String sensorId = reading.getSensor().getSensorId();

                if (reading.getSensor().isEnabled()) {
                    latestBySensor.put(sensorId, reading);
                }
            }
        }

        // 8. Готуємо списки показників
        List<TelemetryReading> readings = new ArrayList<>();
        List<TelemetryReading> changedReadings = new ArrayList<>();

        // 9. Обробляємо телеметрію
        for (Map.Entry<String, JsonNode> entry : request.values().entrySet()) {

            String sensorId = entry.getKey();
            JsonNode value = entry.getValue();

            Sensor sensor = sensorsMap.get(sensorId);

            // Вимкнений датчик:
            // не зберігаємо значення та видаляємо його з кешу
            if (!sensor.isEnabled()) {
                latestBySensor.remove(sensorId);
                continue;
            }

            // Перевіряємо визначення параметра
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

            // Перетворюємо JSON відповідно до типу параметра
            switch (dataType) {

                case NUMBER -> {
                    if (value == null || value.isNull()) {
                        break;
                    }
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

            // Створюємо поточний показник
            TelemetryReading currentReading = new TelemetryReading(
                    sensor,
                    numericValue,
                    booleanValue,
                    textValue,
                    request.measuredAt()
            );

            // Додаємо до поточної телеметрії
            readings.add(currentReading);

            // Отримуємо попередній збережений показник
            TelemetryReading previousReading =
                    previousBySensor.get(sensorId);

            // Перевіряємо зміну значення
            if (previousReading == null
                    || hasValueChanged(previousReading, currentReading)) {

                changedReadings.add(currentReading);
            }

            // Оновлюємо актуальне значення в кеші
            latestBySensor.put(sensorId, currentReading);
        }

        // 10. Пакетне збереження змінених показників
        if (!changedReadings.isEmpty()) {
            telemetryReadingRepository.saveAll(changedReadings);
        }

        // 11. Оновлюємо кеш для Vaadin UI
        telemetryReadingMap.put(
                deviceId,
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

        if (device.getDeviceRegistrationStatus() != DeviceRegistrationStatus.ACTIVE) {
            return List.of();
        }

        List<TelemetryReading> readings =
                new ArrayList<>(telemetryReadingMap.getOrDefault(
                        device.getDeviceId(),
                        List.of()
                ));

        boolean IsOffline = deviceConnectivityService.getDeviceConnectivityStatus(device.getDeviceId())
                == DeviceConnectivityStatus.OFFLINE;

        if (IsOffline) {
            telemetryReadingMap.remove(device.getDeviceId());
            return List.of();
        }

        if (readings.isEmpty()) {
            return List.of();
        }

        // Отримуємо ID всіх датчиків із кешу
        Set<String> sensorIds = readings.stream()
                .map(reading -> reading.getSensor().getSensorId())
                .collect(Collectors.toSet());

        // Одним запитом отримуємо стан усіх датчиків
        Set<String> enabledSensorIds = sensorRepository
                .findAllBySensorIdIn(sensorIds)
                .stream()
                .filter(Sensor::isEnabled)
                .map(Sensor::getSensorId)
                .collect(Collectors.toSet());

        // Видаляємо з кешованого списку вимкнені датчики
        readings.removeIf(reading ->
                !enabledSensorIds.contains(
                        reading.getSensor().getSensorId()
                )
        );

        // Оновлюємо кеш
        telemetryReadingMap.put(
                device.getDeviceId(),
                readings
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

        // Отримуємо всі показники з кешу
        List<TelemetryReading> readings = telemetryReadingMap.values()
                .stream()
                .flatMap(List::stream)
                .toList();

        if (readings.isEmpty()) {
            return List.of();
        }

        // Збираємо унікальні ID датчиків
        Set<String> sensorIds = readings.stream()
                .map(reading -> reading.getSensor().getSensorId())
                .collect(Collectors.toSet());

        // Одним запитом отримуємо всі датчики
        Set<String> enabledSensorIds = sensorRepository
                .findAllBySensorIdIn(sensorIds)
                .stream()
                .filter(Sensor::isEnabled)
                .map(Sensor::getSensorId)
                .collect(Collectors.toSet());

        // Залишаємо тільки активні датчики
        return readings.stream()
                .filter(reading ->
                        enabledSensorIds.contains(
                                reading.getSensor().getSensorId()
                        )
                )
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
