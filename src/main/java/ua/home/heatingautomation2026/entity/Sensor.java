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
        name = "sensors",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_sensors_device_sensor",
                        columnNames = {"device_pk", "sensor_id"}
                )
        },
        indexes = {
                @Index(
                        name = "idx_sensors_device",
                        columnList = "device_pk"
                )
        }
)
public class Sensor {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(nullable = false, updatable = false)
    private UUID id;

    /**
     * Унікальний ідентифікатор фізичного датчика
     * в нашій системі.
     *
     * Наприклад: sensor-flue-01.
     */
    @Column(
            name = "sensor_id",
            nullable = false,
            length = 100
    )
    private String sensorId;

    @Column(
            name = "sensor_name",
            nullable = false,
            length = 150
    )
    private String sensorName;

    /**
     * Тип фізичного датчика.
     *
     * Наприклад: DS18B20, NTC_10K, DHT22.
     * String дозволяє додавати нові типи без зміни enum у БД.
     */
    @Column(
            name = "sensor_type",
            nullable = false,
            length = 50
    )
    private String sensorType;

    /**
     * Кожен датчик належить одному контролеру.
     * Контролер може мати багато датчиків.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "device_pk",
            nullable = false
    )
    private Device device;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parameter_definition_pk")
    private ParameterDefinition parameterDefinition;

    @Column(name = "is_enabled", nullable = false)
    private boolean enabled = true;

    @Column(
            name = "created_at",
            nullable = false,
            updatable = false
    )
    private Instant createdAt;

    public Sensor(
            String sensorId,
            String sensorName,
            String sensorType,
            Device device
    ) {
        this.sensorId = sensorId;
        this.sensorName = sensorName;
        this.sensorType = sensorType;
        this.device = device;
    }

    public Sensor(
            String sensorId,
            String sensorName,
            String sensorType,
            Device device,
            ParameterDefinition parameterDefinition
    ) {
        this.sensorId = sensorId;
        this.sensorName = sensorName;
        this.sensorType = sensorType;
        this.device = device;
        this.parameterDefinition = parameterDefinition;
    }

    public void rename(String sensorName) {
        this.sensorName = sensorName;
    }

    public void enable() {
        this.enabled = true;
    }

    public void disable() {
        this.enabled = false;
    }

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}