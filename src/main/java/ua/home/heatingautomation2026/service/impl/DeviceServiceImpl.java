package ua.home.heatingautomation2026.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ua.home.heatingautomation2026.dto.DeviceRequestRegistrationDto;
import ua.home.heatingautomation2026.enam.DeviceRegistrationStatus;
import ua.home.heatingautomation2026.entity.Device;
import ua.home.heatingautomation2026.exception.EntityNotFoundException;
import ua.home.heatingautomation2026.exception.EntityValidationException;
import ua.home.heatingautomation2026.repository.DeviceRepository;
import ua.home.heatingautomation2026.service.DeviceService;

import java.text.MessageFormat;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DeviceServiceImpl implements DeviceService {
    private static final String MESSAGE = "Device with id {0} not found";
    private final DeviceRepository deviceRepository;

    @Override
    @Transactional
    public Device registerDevice(DeviceRequestRegistrationDto registration) {
        if(deviceRepository.existsByDeviceId(registration.deviceId())){
            throw new EntityValidationException("Device already exists");
        }
        Device device = new Device();
        device.setDeviceId(registration.deviceId());
        device.setDeviceName(registration.deviceName());
        device.setDeviceType(registration.deviceType());
      return deviceRepository.save(device);
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

        if(device.getDeviceRegistrationStatus() != DeviceRegistrationStatus.PENDING) {
            throw new EntityValidationException("Device registration status not PENDING");
        }

        device.setDeviceRegistrationStatus(DeviceRegistrationStatus.ACTIVE);
        return deviceRepository.save(device);
    }

    @Override
    public Device disableDevice(UUID deviceId) {
        var device = deviceRepository.findById(deviceId).orElseThrow(
                () -> new EntityNotFoundException(MessageFormat.format(MESSAGE, deviceId)));

        if(device.getDeviceRegistrationStatus() != DeviceRegistrationStatus.ACTIVE) {
            throw new EntityValidationException("Device status must be ACTIVE");
        }
        device.setDeviceRegistrationStatus(DeviceRegistrationStatus.DISABLED);
        return deviceRepository.save(device);
    }

    @Override
    public Device enableDevice(UUID deviceId) {
        var device = deviceRepository.findById(deviceId).orElseThrow(
                () -> new EntityNotFoundException(MessageFormat.format(MESSAGE, deviceId)));
        if (device.getDeviceRegistrationStatus() != DeviceRegistrationStatus.DISABLED) {
            throw new EntityValidationException("Device status must be DISABLED");
        }
        device.setDeviceRegistrationStatus(DeviceRegistrationStatus.ACTIVE);
        return deviceRepository.save(device);
    }

    @Override
    public Device heartBeat(String deviceId) {
        var device = deviceRepository.findByDeviceId(deviceId).orElseThrow(
                () -> new EntityNotFoundException(MessageFormat.format(MESSAGE, deviceId)));
        device.setLastHeartbeat(Instant.now());
        return deviceRepository.save(device);
    }
}
