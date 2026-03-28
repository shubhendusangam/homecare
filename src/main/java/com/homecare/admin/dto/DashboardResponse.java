package com.homecare.admin.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Data
@Builder
public class DashboardResponse {

    private int todayBookings;
    private int activeBookings;
    private int onlineHelpers;
    private BigDecimal todayRevenue;
    private int pendingVerifications;
    private double avgRating;
    private Map<String, Integer> bookingsByService;
    private List<RevenueDataPoint> revenueChart;

    @Data
    @Builder
    public static class RevenueDataPoint {
        private String date;
        private BigDecimal revenue;
    }
}

