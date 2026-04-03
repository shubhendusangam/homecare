package com.homecare.subscription.dto;

import com.homecare.core.enums.CyclePeriod;
import com.homecare.core.enums.ServiceType;
import com.homecare.core.enums.SubscriptionStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Data
@Builder
public class CustomerSubscriptionResponse {

    private UUID id;
    private UUID customerId;
    private String customerName;

    // Plan summary
    private UUID planId;
    private String planName;
    private ServiceType serviceType;
    private CyclePeriod cyclePeriod;
    private BigDecimal pricePerCycle;

    private SubscriptionStatus status;
    private Instant currentCycleStart;
    private Instant currentCycleEnd;
    private Instant nextRenewalAt;
    private int sessionsPerCycle;
    private int sessionsUsedThisCycle;
    private Instant cancelledAt;
    private String cancellationReason;
    private Instant createdAt;
    private Instant updatedAt;
}

