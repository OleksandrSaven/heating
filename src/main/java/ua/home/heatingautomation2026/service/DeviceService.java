package ua.home.heatingautomation2026.service;

import ua.home.heatingautomation2026.dto.DeviceRequestRegistrationDto;
import ua.home.heatingautomation2026.enam.DeviceRegistrationStatus;
import ua.home.heatingautomation2026.entity.Device;

import java.util.List;
import java.util.UUID;

public interface DeviceService {

    Device registerDevice(DeviceRequestRegistrationDto request);

    List<Device> findAllDevices();

    List<Device> findAllDevicesByDeviceRegistrationStatus(DeviceRegistrationStatus deviceRegistrationStatus);

    Device approveDevice(UUID deviceId);

    Device disableDevice(UUID deviceId);

    Device enableDevice(UUID deviceId);

    Device heartBeat(String deviceId);

    Device findDevice(String deviceId);

    void deleteDevice(UUID deviceId);
}
