package com.homecare.subscription.controller;

import com.homecare.core.dto.ApiResponse;
import com.homecare.core.dto.PagedResponse;
import com.homecare.subscription.dto.SubscriptionPlanResponse;
import com.homecare.subscription.service.SubscriptionService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/subscription-plans")
@RequiredArgsConstructor
public class SubscriptionPlanController {

    private final SubscriptionService subscriptionService;

    @GetMapping
    public ResponseEntity<ApiResponse<PagedResponse<SubscriptionPlanResponse>>> listActivePlans(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        PagedResponse<SubscriptionPlanResponse> plans = subscriptionService
                .listActivePlans(PageRequest.of(page, size));
        return ResponseEntity.ok(ApiResponse.ok(plans));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<SubscriptionPlanResponse>> getPlan(@PathVariable UUID id) {
        SubscriptionPlanResponse plan = subscriptionService.getPlan(id);
        return ResponseEntity.ok(ApiResponse.ok(plan));
    }
}

