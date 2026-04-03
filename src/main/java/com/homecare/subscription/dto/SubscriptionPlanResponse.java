package com.homecare.subscription.dto;

import com.homecare.core.enums.CyclePeriod;
import com.homecare.core.enums.ServiceType;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Data
@Builder
public class SubscriptionPlanResponse {

    private UUID id;
    private String name;
    private String description;
    private ServiceType serviceType;
    private CyclePeriod cyclePeriod;
    private int sessionsPerCycle;
    private int durationHoursPerSession;
    private BigDecimal pricePerCycle;
    private BigDecimal discountPercentage;
    private boolean active;
    private Instant createdAt;
    private Instant updatedAt;
}

