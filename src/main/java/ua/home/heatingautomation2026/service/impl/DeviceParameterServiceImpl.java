package ua.home.heatingautomation2026.service.impl;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import ua.home.heatingautomation2026.entity.Device;
import ua.home.heatingautomation2026.entity.DeviceParameter;
import ua.home.heatingautomation2026.exception.EntityNotFoundException;
import ua.home.heatingautomation2026.exception.EntityValidationException;
import ua.home.heatingautomation2026.repository.DeviceParameterRepository;
import ua.home.heatingautomation2026.repository.DeviceRepository;
import ua.home.heatingautomation2026.repository.ParameterDefinitionRepository;
import ua.home.heatingautomation2026.service.DeviceParameterService;

import java.util.List;

@Service
@AllArgsConstructor
public class DeviceParameterServiceImpl implements DeviceParameterService {
    private final DeviceRepository deviceRepository;
    private final DeviceParameterRepository deviceParameterRepository;
    private final ParameterDefinitionRepository parameterDefinitionRepository;

    @Override
    public DeviceParameter assignParameter(String deviceId, String parameterCode) {
        var device = findDevice(deviceId);
        var parameter = parameterDefinitionRepository.findByCode(parameterCode).orElseThrow(
                () -> new EntityNotFoundException("Parameter with code: " + parameterCode + " not found"));

        if (deviceParameterRepository.existsByDevice_IdAndParameterDefinition_Id(device.getId(), parameter.getId())) {
            throw new EntityValidationException("Parameter with code: " + parameterCode +
                    " already assigned to device with id: " + deviceId);
        }
        DeviceParameter deviceParameter = new DeviceParameter(
                device, parameter
        );

        return deviceParameterRepository.save(deviceParameter);
    }

    @Override
    public List<DeviceParameter> getDeviceParameters(String deviceId) {
        var device = findDevice(deviceId);

        return deviceParameterRepository.findAllByDevice_DeviceId(device.getDeviceId());
    }

    @Override
    public DeviceParameter setParameterStatus(String deviceId, String parameterCode, boolean status) {
        var device = findDevice(deviceId);

        var parameter = deviceParameterRepository.findByDevice_DeviceIdAndParameterDefinition_Code(device.getDeviceId(),
                parameterCode).orElseThrow(
                () -> new EntityNotFoundException("Parameter with code: " + parameterCode + " not found")
        );

        if (status) {
            parameter.enable();
        } else {
            parameter.disable();
        }

        return deviceParameterRepository.save(parameter);
    }

    private Device findDevice(String deviceId) {
        return deviceRepository.findByDeviceId(deviceId).orElseThrow(
                () -> new EntityNotFoundException("Device with id: " + deviceId + " not found"));
    }
}
