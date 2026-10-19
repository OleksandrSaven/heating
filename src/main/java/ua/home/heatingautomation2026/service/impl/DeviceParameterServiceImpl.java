package ua.home.heatingautomation2026.service.impl;

import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ua.home.heatingautomation2026.entity.Device;
import ua.home.heatingautomation2026.entity.DeviceParameter;
import ua.home.heatingautomation2026.entity.Sensor;
import ua.home.heatingautomation2026.exception.EntityNotFoundException;
import ua.home.heatingautomation2026.exception.EntityValidationException;
import ua.home.heatingautomation2026.repository.DeviceParameterRepository;
import ua.home.heatingautomation2026.repository.DeviceRepository;
import ua.home.heatingautomation2026.repository.ParameterDefinitionRepository;
import ua.home.heatingautomation2026.repository.SensorRepository;
import ua.home.heatingautomation2026.service.DeviceParameterService;

import java.util.List;

@Service
@AllArgsConstructor
public class DeviceParameterServiceImpl implements DeviceParameterService {
    private final DeviceRepository deviceRepository;
    private final DeviceParameterRepository deviceParameterRepository;
    private final ParameterDefinitionRepository parameterDefinitionRepository;
    private final SensorRepository sensorRepository;

    @Override
    @Transactional
    public DeviceParameter assignParameter(String deviceId, String parameterCode) {
        var device = findDevice(deviceId);
        var parameter = parameterDefinitionRepository.findByCode(parameterCode).orElseThrow(
                () -> new EntityNotFoundException("Parameter with code: " + parameterCode + " not found"));

  /*      if (deviceParameterRepository.existsByDevice_IdAndParameterDefinition_Id(device.getId(), parameter.getId())) {
            throw new EntityValidationException("Parameter with code: " + parameterCode +
                    " already assigned to device with id: " + deviceId);
        }*/
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

    @Override
    @Transactional
    public void assignSensor(
            String deviceId,
            String parameterCode,
            String sensorId
    ) {
        // Знаходимо призначення параметра конкретному пристрою.
        DeviceParameter assignment =
                deviceParameterRepository
                        .findByDevice_DeviceIdAndParameterDefinition_Code(
                                deviceId,
                                parameterCode
                        )
                        .orElseThrow(() ->
                                new EntityNotFoundException("Device with code: " + parameterCode + " not found")
                        );

        // Знаходимо фізичний датчик.
        Sensor sensor = sensorRepository.findBySensorId(sensorId)
                .orElseThrow(() ->
                        new EntityNotFoundException("Sensor with id: " + sensorId + " not found")
                );

        // Забороняємо прив'язування вимкненого датчика.
        if (!sensor.isEnabled()) {
            throw new EntityValidationException(
                    "Sensor " + sensorId + " is disabled"
            );
        }

        // Датчик повинен належати саме цьому ESP32.
        if (!sensor.getDevice().getId()
                .equals(assignment.getDevice().getId())) {

            throw new EntityValidationException(
                    "Sensor " + sensorId
                            + " belongs to another device"
            );
        }

        // Якщо цей датчик уже прив'язаний до іншого призначення,
        // повторно використовувати його не можна.
        if (deviceParameterRepository.existsBySensor_Id(sensor.getId())) {

            // Повторний запит із тим самим датчиком для того самого
            // призначення нічого не змінює.
            if (assignment.getSensor() != null
                    && assignment.getSensor().getId()
                    .equals(sensor.getId())) {
                return;
            }

            throw new EntityValidationException(
                    "Sensor " + sensorId
                            + " is already assigned to another parameter"
            );
        }

        assignment.assignSensor(sensor);

        deviceParameterRepository.save(assignment);
    }

    private Device findDevice(String deviceId) {
        return deviceRepository.findByDeviceId(deviceId).orElseThrow(
                () -> new EntityNotFoundException("Device with id: " + deviceId + " not found"));
    }
}
