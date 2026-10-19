package ua.home.heatingautomation2026.service;

import ua.home.heatingautomation2026.entity.Sensor;

import java.util.List;
import java.util.UUID;

public interface SensorService {
    List<Sensor> findAll();

    Sensor registerSensor(
            String sensorId,
            String sensorName,
            String sensorType,
            String deviceId,
            String parameterCode);

    void setEnabled(UUID sensorUuid, boolean enabled);

    List<Sensor> findEnabledByDeviceId(String deviceId);

    List<Sensor> findAllByDeviceId(String deviceId);
}
