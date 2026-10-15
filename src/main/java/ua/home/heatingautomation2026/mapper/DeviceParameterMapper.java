package ua.home.heatingautomation2026.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import ua.home.heatingautomation2026.dto.DeviceParameterResponse;
import ua.home.heatingautomation2026.entity.DeviceParameter;

import java.util.List;

@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public interface DeviceParameterMapper {

    @Mapping(target = "deviceId", source = "device.deviceId")
    @Mapping(target = "deviceName", source = "device.deviceName")
    @Mapping(target = "parameterId", source = "parameterDefinition.id")
    @Mapping(target = "parameterCode", source = "parameterDefinition.code")
    @Mapping(target = "parameterName", source = "parameterDefinition.name")
    @Mapping(target = "dataType", source = "parameterDefinition.parameterDataType")
    @Mapping(target = "unit", source = "parameterDefinition.unit")
    DeviceParameterResponse toResponse(DeviceParameter deviceParameter);

    List<DeviceParameterResponse> toResponseList(
            List<DeviceParameter> deviceParameters
    );
}