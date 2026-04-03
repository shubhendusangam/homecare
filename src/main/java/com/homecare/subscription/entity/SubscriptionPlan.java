package com.homecare.subscription.entity;

import com.homecare.core.entity.BaseEntity;
import com.homecare.core.enums.CyclePeriod;
import com.homecare.core.enums.ServiceType;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "subscription_plans", indexes = {
        @Index(name = "idx_plan_service_active", columnList = "service_type, active")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubscriptionPlan extends BaseEntity {

    @Column(nullable = false)
    private String name;

    @Column(length = 2000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "service_type", nullable = false)
    private ServiceType serviceType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CyclePeriod cyclePeriod;

    @Column(nullable = false)
    private int sessionsPerCycle;

    @Column(nullable = false)
    private int durationHoursPerSession;

    @Column(precision = 10, scale = 2, nullable = false)
    private BigDecimal pricePerCycle;

    @Column(precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal discountPercentage = BigDecimal.ZERO;

    @Builder.Default
    private boolean active = true;
}

