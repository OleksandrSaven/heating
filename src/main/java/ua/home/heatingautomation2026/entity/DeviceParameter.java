package ua.home.heatingautomation2026.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.Instant;
import java.util.UUID;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(
        name = "device_parameters",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_device_parameter",
                        columnNames = {"device_pk", "parameter_pk"}
                )
        }
)
public class DeviceParameter {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "device_pk", nullable = false)
    private Device device;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "parameter_pk", nullable = false)
    private ParameterDefinition parameterDefinition;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "sensor_pk",
            unique = true
    )
    private Sensor sensor;

    @Column(name = "is_enabled", nullable = false)
    private boolean enabled = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public DeviceParameter(
            Device device,
            ParameterDefinition parameterDefinition
    ) {
        this.device = device;
        this.parameterDefinition = parameterDefinition;
    }

    public DeviceParameter(
            Device device,
            ParameterDefinition parameterDefinition,
            Sensor sensor
    ) {
        this.device = device;
        this.parameterDefinition = parameterDefinition;
        this.sensor = sensor;
    }

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    public void enable() {
        this.enabled = true;
    }

    public void disable() {
        this.enabled = false;
    }

    public void assignSensor(Sensor sensor) {
        this.sensor = sensor;
    }

    public void unassignSensor() {
        this.sensor = null;
    }
}