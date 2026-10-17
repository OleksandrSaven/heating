package ua.home.heatingautomation2026.service.impl;

import org.springframework.stereotype.Service;
import ua.home.heatingautomation2026.enam.DeviceConnectivityStatus;
import ua.home.heatingautomation2026.service.DeviceConnectivityService;

import java.time.Duration;
import java.time.Instant;

@Service
public class DeviceConnectivityServiceImpl implements DeviceConnectivityService {
    private static final Duration CONNECTION_TIMEOUT = Duration.ofSeconds(90);

    @Override
    public DeviceConnectivityStatus getDeviceConnectivityStatus(Instant lastHeartbeat) {
        if (lastHeartbeat == null) {
            return DeviceConnectivityStatus.NEVER_CONNECTED;
        }
        var offlineThreshold = Instant.now().minus(CONNECTION_TIMEOUT);

        if (lastHeartbeat.isBefore(offlineThreshold)) {
            return DeviceConnectivityStatus.OFFLINE;
        }
        return DeviceConnectivityStatus.ONLINE;
    }
}
