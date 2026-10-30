package ua.home.heatingautomation2026.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ua.home.heatingautomation2026.dto.DeviceRequestRegistrationDto;
import ua.home.heatingautomation2026.dto.SensorRegistrationDto;
import ua.home.heatingautomation2026.enam.DeviceRegistrationStatus;
import ua.home.heatingautomation2026.entity.Device;
import ua.home.heatingautomation2026.entity.ParameterDefinition;
import ua.home.heatingautomation2026.entity.Sensor;
import ua.home.heatingautomation2026.exception.EntityNotFoundException;
import ua.home.heatingautomation2026.exception.EntityValidationException;
import ua.home.heatingautomation2026.repository.DeviceRepository;
import ua.home.heatingautomation2026.repository.ParameterDefinitionRepository;
import ua.home.heatingautomation2026.repository.SensorRepository;
import ua.home.heatingautomation2026.repository.TelemetryReadingRepository;
import ua.home.heatingautomation2026.service.DeviceService;
import ua.home.heatingautomation2026.service.impl.handler.MqttRegistrationStatusPublisher;

import java.text.MessageFormat;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DeviceServiceImpl implements DeviceService {
    private static final String MESSAGE = "Device with id {0} not found";
    private final DeviceRepository deviceRepository;
    private final TelemetryReadingRepository telemetryReadingRepository;
    private final SensorRepository sensorRepository;
    private final ParameterDefinitionRepository parameterDefinitionRepository;
    private final MqttRegistrationStatusPublisher mqttRegistrationStatusPublisher;

    @Override
    @Transactional
    public Device registerDevice(DeviceRequestRegistrationDto registration) {

        if (deviceRepository.existsByDeviceId(registration.deviceId())) {
            throw new EntityValidationException("Device already exists");
        }

        Device device = new Device();

        device.setDeviceId(registration.deviceId());
        device.setDeviceName(registration.deviceName());
        device.setDeviceType(registration.deviceType());
        device.setLastHeartbeat(Instant.now());

        // Новий пристрій матиме статус PENDING.
        device = deviceRepository.save(device);

        // Автоматично створюємо датчики.
        for (SensorRegistrationDto sensorDto : registration.sensors()) {

            ParameterDefinition parameterDefinition = parameterDefinitionRepository.findByCode(sensorDto.parameterCode())
                    .orElseThrow(
                            () -> new EntityValidationException("Unknown parameter code: " + sensorDto.parameterCode()));

            Sensor sensor = new Sensor(
                    sensorDto.sensorId(),
                    sensorDto.sensorName(),
                    sensorDto.sensorType(),
                    device,
                    parameterDefinition);

            sensorRepository.save(sensor);
        }

        return device;
    }

    @Override
    public List<Device> findAllDevices() {
        return deviceRepository.findAll();
    }

    @Override
    public List<Device> findAllDevicesByDeviceRegistrationStatus(DeviceRegistrationStatus deviceRegistrationStatus) {
        return deviceRepository.findAllDevicesByDeviceRegistrationStatus(deviceRegistrationStatus);
    }

    @Override
    public Device approveDevice(UUID deviceId) {
        var device = deviceRepository.findById(deviceId).orElseThrow(
                () -> new EntityNotFoundException(MessageFormat.format(MESSAGE, deviceId)));

        if (device.getDeviceRegistrationStatus() != DeviceRegistrationStatus.PENDING) {
            throw new EntityValidationException("Device registration status not PENDING");
        }

        device.setDeviceRegistrationStatus(DeviceRegistrationStatus.ACTIVE);
        Device savedDevice = deviceRepository.save(device);
        mqttRegistrationStatusPublisher.publish(savedDevice);

        return savedDevice;
    }

    @Override
    public Device disableDevice(UUID deviceId) {
        var device = deviceRepository.findById(deviceId).orElseThrow(
                () -> new EntityNotFoundException(MessageFormat.format(MESSAGE, deviceId)));

        if (device.getDeviceRegistrationStatus() != DeviceRegistrationStatus.ACTIVE) {
            throw new EntityValidationException("Device status must be ACTIVE");
        }
        device.setDeviceRegistrationStatus(DeviceRegistrationStatus.DISABLED);
        Device savedDevice = deviceRepository.save(device);
        mqttRegistrationStatusPublisher.publish(savedDevice);

        return savedDevice;
    }

    @Override
    public Device enableDevice(UUID deviceId) {
        var device = deviceRepository.findById(deviceId).orElseThrow(
                () -> new EntityNotFoundException(MessageFormat.format(MESSAGE, deviceId)));
        if (device.getDeviceRegistrationStatus() != DeviceRegistrationStatus.DISABLED) {
            throw new EntityValidationException("Device status must be DISABLED");
        }
        device.setDeviceRegistrationStatus(DeviceRegistrationStatus.ACTIVE);
        Device savedDevice = deviceRepository.save(device);
        mqttRegistrationStatusPublisher.publish(savedDevice);

        return savedDevice;
    }

    @Override
    public Device heartBeat(String deviceId) {
        var device = deviceRepository.findByDeviceId(deviceId).orElseThrow(
                () -> new EntityNotFoundException(MessageFormat.format(MESSAGE, deviceId)));
        device.setLastHeartbeat(Instant.now());
        return deviceRepository.save(device);
    }

    @Override
    public Device findDevice(String deviceId) {
        return deviceRepository.findByDeviceId(deviceId).orElseThrow(
                () -> new EntityNotFoundException(MessageFormat.format(MESSAGE, deviceId)));
    }

    @Override
    @Transactional
    public void deleteDevice(UUID deviceId) {
        Device device = deviceRepository.findById(deviceId).orElseThrow(
                () -> new EntityNotFoundException("Device not found"));

        // 1. Видаляємо всю історію телеметрії пристрою.
        telemetryReadingRepository.deleteAllByDeviceId(deviceId);

        //2 Видаляємо всі прив'язані датчики
        sensorRepository.deleteAllByDeviceId(deviceId);

        // 3. Видаляємо сам пристрій.
        deviceRepository.delete(device);
    }
}
