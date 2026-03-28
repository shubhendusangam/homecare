package com.homecare.admin.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
public class RevenueAnalyticsResponse {

    private BigDecimal totalRevenue;
    private BigDecimal platformFee;
    private List<DashboardResponse.RevenueDataPoint> data;
}

