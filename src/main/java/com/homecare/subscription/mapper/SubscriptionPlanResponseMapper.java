package com.homecare.subscription.mapper;

import com.homecare.subscription.dto.SubscriptionPlanResponse;
import com.homecare.subscription.entity.SubscriptionPlan;
import org.springframework.stereotype.Component;

@Component
public class SubscriptionPlanResponseMapper {

    public SubscriptionPlanResponse toDto(SubscriptionPlan plan) {
        return SubscriptionPlanResponse.builder()
                .id(plan.getId())
                .name(plan.getName())
                .description(plan.getDescription())
                .serviceType(plan.getServiceType())
                .cyclePeriod(plan.getCyclePeriod())
                .sessionsPerCycle(plan.getSessionsPerCycle())
                .durationHoursPerSession(plan.getDurationHoursPerSession())
                .pricePerCycle(plan.getPricePerCycle())
                .discountPercentage(plan.getDiscountPercentage())
                .active(plan.isActive())
                .createdAt(plan.getCreatedAt())
                .updatedAt(plan.getUpdatedAt())
                .build();
    }
}

