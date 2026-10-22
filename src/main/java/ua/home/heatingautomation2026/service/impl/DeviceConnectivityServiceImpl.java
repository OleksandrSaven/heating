package ua.home.heatingautomation2026.service.impl;

import org.springframework.stereotype.Service;
import ua.home.heatingautomation2026.enam.DeviceConnectivityStatus;
import ua.home.heatingautomation2026.service.DeviceConnectivityService;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Service
public class DeviceConnectivityServiceImpl implements DeviceConnectivityService {
    private static final Duration CONNECTION_TIMEOUT = Duration.ofSeconds(90);

    private final ConcurrentMap<String, Instant> lastActivity =
            new ConcurrentHashMap<>();

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

    @Override
    public void markOnline(String deviceId) {
        if (deviceId == null || deviceId.isBlank()) {
            return;
        }

        lastActivity.put(deviceId, Instant.now());

    }

    @Override
    public void markOffline(String deviceId) {
        if (deviceId == null || deviceId.isBlank()) {
            return;
        }

        //lastActivity.remove(deviceId);

    }

    @Override
    public Instant getLastActivity(String deviceId) {
        return lastActivity.get(deviceId);    }

    @Override
    public DeviceConnectivityStatus getDeviceConnectivityStatus(String deviceId) {

        Instant lastSeen = getLastActivity(deviceId);

        if (lastSeen == null) {
            return DeviceConnectivityStatus.NEVER_CONNECTED;
        }

        if (lastSeen.isBefore(
                Instant.now().minus(CONNECTION_TIMEOUT))) {
            return DeviceConnectivityStatus.OFFLINE;
        }

        return DeviceConnectivityStatus.ONLINE;
    }
}
