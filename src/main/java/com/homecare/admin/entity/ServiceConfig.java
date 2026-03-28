package com.homecare.admin.entity;

import com.homecare.core.entity.BaseEntity;
import com.homecare.core.enums.ServiceType;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "service_config", uniqueConstraints = {
        @UniqueConstraint(name = "uk_service_config_type", columnNames = "service_type")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ServiceConfig extends BaseEntity {

    @Enumerated(EnumType.STRING)
    @Column(name = "service_type", nullable = false)
    private ServiceType serviceType;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private double basePrice;

    @Column(nullable = false)
    private double perHourPrice;

    private String icon;

    @Builder.Default
    private boolean active = true;
}

