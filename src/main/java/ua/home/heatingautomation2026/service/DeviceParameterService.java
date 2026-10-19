package ua.home.heatingautomation2026.service;

import ua.home.heatingautomation2026.entity.DeviceParameter;

import java.util.List;

public interface DeviceParameterService {

    DeviceParameter assignParameter(String deviceId, String parameterCode);

    List<DeviceParameter> getDeviceParameters(String deviceId);

    DeviceParameter setParameterStatus(String deviceId, String parameterCode, boolean status);

    void assignSensor(String deviceId, String parameterCode, String sensorId);
}
