package com.homecare.admin.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
public class CustomerAnalyticsItem {
    private UUID userId;
    private String name;
    private String email;
    private int totalBookings;
    private BigDecimal totalSpent;
}

