package com.homecare.admin.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
public class HelperAnalyticsItem {
    private UUID helperId;
    private UUID userId;
    private String name;
    private double rating;
    private int totalJobsCompleted;
    private BigDecimal totalEarnings;
}

