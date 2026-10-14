package ua.home.heatingautomation2026.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ua.home.heatingautomation2026.enam.DeviceRegistrationStatus;
import ua.home.heatingautomation2026.entity.Device;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DeviceRepository extends JpaRepository<Device, UUID> {

    Optional<Device> findByDeviceId(String deviceId);

    boolean existsByDeviceId(String deviceId);

    List<Device> findAllDevicesByDeviceRegistrationStatus(DeviceRegistrationStatus deviceRegistrationStatus);
}
