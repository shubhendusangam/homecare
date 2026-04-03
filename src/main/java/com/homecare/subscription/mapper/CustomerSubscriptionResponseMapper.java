package com.homecare.subscription.mapper;

import com.homecare.subscription.dto.CustomerSubscriptionResponse;
import com.homecare.subscription.entity.CustomerSubscription;
import com.homecare.subscription.entity.SubscriptionPlan;
import org.springframework.stereotype.Component;

@Component
public class CustomerSubscriptionResponseMapper {

    public CustomerSubscriptionResponse toDto(CustomerSubscription sub) {
        SubscriptionPlan plan = sub.getPlan();
        return CustomerSubscriptionResponse.builder()
                .id(sub.getId())
                .customerId(sub.getCustomer().getId())
                .customerName(sub.getCustomer().getName())
                .planId(plan.getId())
                .planName(plan.getName())
                .serviceType(plan.getServiceType())
                .cyclePeriod(plan.getCyclePeriod())
                .pricePerCycle(plan.getPricePerCycle())
                .status(sub.getStatus())
                .currentCycleStart(sub.getCurrentCycleStart())
                .currentCycleEnd(sub.getCurrentCycleEnd())
                .nextRenewalAt(sub.getNextRenewalAt())
                .sessionsPerCycle(plan.getSessionsPerCycle())
                .sessionsUsedThisCycle(sub.getSessionsUsedThisCycle())
                .cancelledAt(sub.getCancelledAt())
                .cancellationReason(sub.getCancellationReason())
                .createdAt(sub.getCreatedAt())
                .updatedAt(sub.getUpdatedAt())
                .build();
    }
}

