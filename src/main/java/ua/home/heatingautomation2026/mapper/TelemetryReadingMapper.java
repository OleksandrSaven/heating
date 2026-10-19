package ua.home.heatingautomation2026.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import ua.home.heatingautomation2026.dto.TelemetryReadingResponse;
import ua.home.heatingautomation2026.entity.TelemetryReading;

@Mapper(componentModel = "spring", unmappedSourcePolicy = ReportingPolicy.IGNORE)
public interface TelemetryReadingMapper {

   @Mapping(target = "deviceId", source = "sensor.device.deviceId")
   @Mapping(target = "deviceName", source = "sensor.device.deviceName")
   @Mapping(target = "parameterCode", source = "sensor.parameterDefinition.code")
   @Mapping(target = "parameterName", source = "sensor.parameterDefinition.name")
   @Mapping(target = "dataType", source = "sensor.parameterDefinition.parameterDataType")
   @Mapping(target = "unit", source = "sensor.parameterDefinition.unit")
   @Mapping(target = "sensorId", source = "sensor.sensorId")
   @Mapping(target = "sensorName", source = "sensor.sensorName")
   TelemetryReadingResponse toTelemetryReadingResponse(TelemetryReading telemetryReading);
}
