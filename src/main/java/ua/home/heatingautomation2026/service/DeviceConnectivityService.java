package ua.home.heatingautomation2026.service;

import ua.home.heatingautomation2026.enam.DeviceConnectivityStatus;

import java.time.Instant;

public interface DeviceConnectivityService {

    DeviceConnectivityStatus getDeviceConnectivityStatus(Instant lastHeartbeat);
}
