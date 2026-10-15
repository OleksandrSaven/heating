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
import ua.home.heatingautomation2026.enam.ParameterDataType;

@Setter
@Getter
@Entity
@Table(name = "parameter_definitions")
public class ParameterDefinition {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String code;

    @Column(nullable = false)
    private String name;

    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "data_type")
    private ParameterDataType parameterDataType;

    @Column(length = 30)
    private String unit;
}
