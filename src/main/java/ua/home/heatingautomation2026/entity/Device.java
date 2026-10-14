package ua.home.heatingautomation2026.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import ua.home.heatingautomation2026.enam.DeviceRegistrationStatus;
import ua.home.heatingautomation2026.enam.DeviceType;

import java.time.Instant;
import java.util.UUID;

@Setter
@Getter
@Table(name = "devices")
@Entity
public class Device {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(name = "device_id", nullable = false, unique = true, length = 100)
    private String deviceId;

    @Column(name = "device_name",  nullable = false, length = 100)
    private String deviceName;

    @Enumerated(EnumType.STRING)
    @Column(name = "device_type", nullable = false)
    private DeviceType deviceType;

    @Enumerated(EnumType.STRING)
    @Column(name = "registration_status",nullable = false)
    private DeviceRegistrationStatus deviceRegistrationStatus = DeviceRegistrationStatus.PENDING;

    @Column(name = "last_heartbeat_at")
    private Instant lastHeartbeat;
    }
