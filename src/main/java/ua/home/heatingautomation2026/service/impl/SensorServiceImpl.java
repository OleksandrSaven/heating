package ua.home.heatingautomation2026.service.impl;


import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ua.home.heatingautomation2026.entity.Device;
import ua.home.heatingautomation2026.entity.ParameterDefinition;
import ua.home.heatingautomation2026.entity.Sensor;
import ua.home.heatingautomation2026.exception.EntityNotFoundException;
import ua.home.heatingautomation2026.exception.EntityValidationException;
import ua.home.heatingautomation2026.repository.DeviceRepository;
import ua.home.heatingautomation2026.repository.ParameterDefinitionRepository;
import ua.home.heatingautomation2026.repository.SensorRepository;
import ua.home.heatingautomation2026.service.SensorService;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class SensorServiceImpl  implements SensorService {

    private final SensorRepository sensorRepository;
    private final DeviceRepository deviceRepository;
    private final ParameterDefinitionRepository parameterDefinitionRepository;

    public List<Sensor> findAll() {
        return sensorRepository.findAllByOrderBySensorIdAsc();
    }

    @Transactional
    public Sensor registerSensor(
            String sensorId,
            String sensorName,
            String sensorType,
            String deviceId,
            String parameterCode
    ) {
        if (sensorId == null || sensorId.isBlank()) {
            throw new IllegalArgumentException(
                    "Sensor ID cannot be empty"
            );
        }

        if (sensorName == null || sensorName.isBlank()) {
            throw new IllegalArgumentException(
                    "Sensor name cannot be empty"
            );
        }

        if (sensorType == null || sensorType.isBlank()) {
            throw new IllegalArgumentException(
                    "Sensor type cannot be empty"
            );
        }

        if (deviceId == null || deviceId.isBlank()) {
            throw new IllegalArgumentException(
                    "Device ID cannot be empty"
            );
        }

        if (parameterCode == null || parameterCode.isBlank()) {
            throw new IllegalArgumentException(
                    "Parameter code cannot be empty"
            );
        }

        sensorId = sensorId.trim();
        sensorName = sensorName.trim();
        sensorType = sensorType.trim();
        deviceId = deviceId.trim();
        parameterCode = parameterCode.trim().toLowerCase(Locale.ROOT);

        if (sensorId.length() > 100
                || sensorName.length() > 150
                || sensorType.length() > 50) {
            throw new IllegalArgumentException(
                    "Sensor ID, name or type exceeds the allowed length"
            );
        }

        if (sensorRepository.existsBySensorId(sensorId)) {
            throw new EntityValidationException(
                    "Sensor ID already exists: " + sensorId
            );
        }

        Device device = deviceRepository.findByDeviceId(deviceId)
                .orElseThrow(() ->
                        new EntityNotFoundException("Device not found")
                );

        ParameterDefinition parameterDefinition =
                parameterDefinitionRepository.findByCode(parameterCode)
                        .orElseThrow(() ->
                                new EntityValidationException("Code does not exist")
                        );

        Sensor sensor = new Sensor(
                sensorId,
                sensorName,
                sensorType,
                device,
                parameterDefinition
        );

        return sensorRepository.save(sensor);
    }
    @Transactional
    public void setEnabled(UUID sensorUuid, boolean enabled) {

        Sensor sensor = sensorRepository.findById(sensorUuid)
                .orElseThrow(() ->
                        new EntityNotFoundException("Sensor not found " +
                                sensorUuid.toString()
                        )
                );

        if (enabled) {
            sensor.enable();
        } else {
            sensor.disable();
        }

        sensorRepository.save(sensor);
    }

    @Override
    public List<Sensor> findEnabledByDeviceId(String deviceId) {
        return sensorRepository
                .findAllByDevice_DeviceIdAndEnabledTrueOrderBySensorIdAsc(
                        deviceId
                );
    }

    @Override
    public List<Sensor> findAllByDeviceId(String deviceId) {
        return sensorRepository.findAllByDevice_DeviceIdOrderBySensorIdAsc(deviceId);
    }
}
