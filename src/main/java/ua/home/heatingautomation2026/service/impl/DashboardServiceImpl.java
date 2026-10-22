package ua.home.heatingautomation2026.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ua.home.heatingautomation2026.dto.DashboardResponse;
import ua.home.heatingautomation2026.enam.DeviceRegistrationStatus;
import ua.home.heatingautomation2026.mapper.TelemetryReadingMapper;
import ua.home.heatingautomation2026.repository.DeviceRepository;
import ua.home.heatingautomation2026.repository.TelemetryReadingRepository;
import ua.home.heatingautomation2026.service.DashboardService;
import ua.home.heatingautomation2026.service.TelemetryService;

@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {
    private final DeviceRepository deviceRepository;
    private final TelemetryReadingRepository telemetryReadingRepository;
    private final TelemetryReadingMapper telemetryReadingMapper;
    private final TelemetryService telemetryService;

    @Override
    public DashboardResponse getDashboard() {
        var totalDevice = deviceRepository.count();
        var activeDevice = deviceRepository.countByDeviceRegistrationStatus(DeviceRegistrationStatus.ACTIVE);

        /*var readings = telemetryReadingRepository.findTop100ByOrderByReceivedAtDesc().stream()
                .map(telemetryReadingMapper::toTelemetryReadingResponse)
                .toList();
*/
        var readings = telemetryService.getTelemetryReadings();

        return new DashboardResponse(
                totalDevice,
                activeDevice,
                readings);
    }
}
