package ua.home.heatingautomation2026.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(
        name = "telemetry_readings",
        indexes = {
                @Index(
                        name = "idx_telemetry_sensor_received",
                        columnList = "sensor_pk, received_at"
                )
        }
)
public class TelemetryReading {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "sensor_pk",
            nullable = false,
            updatable = false
    )
    private Sensor sensor;

    @Column(name = "numeric_value", precision = 19, scale = 6)
    private BigDecimal numericValue;

    @Column(name = "boolean_value")
    private Boolean booleanValue;

    @Column(name = "text_value", length = 2000)
    private String textValue;

    @Column(name = "measured_at")
    private Instant measuredAt;

    @Column(
            name = "received_at",
            nullable = false,
            updatable = false
    )
    private Instant receivedAt;

    public TelemetryReading(
            Sensor sensor,
            BigDecimal numericValue,
            Boolean booleanValue,
            String textValue,
            Instant measuredAt
    ) {
        this.sensor = sensor;
        this.numericValue = numericValue;
        this.booleanValue = booleanValue;
        this.textValue = textValue;
        this.measuredAt = measuredAt;
    }

    @PrePersist
    protected void onCreate() {
        if (receivedAt == null) {
            receivedAt = Instant.now();
        }
    }
}
