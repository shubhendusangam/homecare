package com.homecare.admin.controller;

import com.homecare.admin.dto.*;
import com.homecare.admin.service.AdminService;
import com.homecare.core.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class AdminDashboardController {

    private final AdminService adminService;

    @GetMapping("/dashboard")
    public ResponseEntity<ApiResponse<DashboardResponse>> getDashboard() {
        DashboardResponse dashboard = adminService.getDashboard();
        return ResponseEntity.ok(ApiResponse.ok(dashboard));
    }

    @GetMapping("/analytics/bookings")
    public ResponseEntity<ApiResponse<BookingAnalyticsResponse>> getBookingAnalytics(
            @RequestParam(required = false) Instant from,
            @RequestParam(required = false) Instant to,
            @RequestParam(required = false, defaultValue = "DAY") String groupBy) {
        BookingAnalyticsResponse analytics = adminService.getBookingAnalytics(from, to, groupBy);
        return ResponseEntity.ok(ApiResponse.ok(analytics));
    }

    @GetMapping("/analytics/revenue")
    public ResponseEntity<ApiResponse<RevenueAnalyticsResponse>> getRevenueAnalytics(
            @RequestParam(required = false) Instant from,
            @RequestParam(required = false) Instant to) {
        RevenueAnalyticsResponse analytics = adminService.getRevenueAnalytics(from, to);
        return ResponseEntity.ok(ApiResponse.ok(analytics));
    }

    @GetMapping("/analytics/helpers")
    public ResponseEntity<ApiResponse<List<HelperAnalyticsItem>>> getHelperAnalytics(
            @RequestParam(required = false, defaultValue = "topRated") String metric,
            @RequestParam(required = false, defaultValue = "10") int limit) {
        List<HelperAnalyticsItem> analytics = adminService.getHelperAnalytics(metric, limit);
        return ResponseEntity.ok(ApiResponse.ok(analytics));
    }

    @GetMapping("/analytics/customers")
    public ResponseEntity<ApiResponse<List<CustomerAnalyticsItem>>> getCustomerAnalytics(
            @RequestParam(required = false, defaultValue = "mostActive") String metric,
            @RequestParam(required = false, defaultValue = "10") int limit) {
        List<CustomerAnalyticsItem> analytics = adminService.getCustomerAnalytics(metric, limit);
        return ResponseEntity.ok(ApiResponse.ok(analytics));
    }
}

