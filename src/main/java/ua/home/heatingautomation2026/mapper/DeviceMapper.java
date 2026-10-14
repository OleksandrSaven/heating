package ua.home.heatingautomation2026.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import ua.home.heatingautomation2026.dto.DeviceRequestRegistrationDto;
import ua.home.heatingautomation2026.dto.DeviceResponse;
import ua.home.heatingautomation2026.entity.Device;

@Mapper(componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface DeviceMapper {

    DeviceResponse toResponse(Device device);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "deviceRegistrationStatus", ignore = true)
    @Mapping(target = "lastHeartbeat", ignore = true)
    Device toEntity(DeviceRequestRegistrationDto requestDto);
}
