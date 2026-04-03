package com.homecare.subscription.controller;

import com.homecare.core.dto.ApiResponse;
import com.homecare.core.dto.PagedResponse;
import com.homecare.subscription.dto.CreateSubscriptionPlanRequest;
import com.homecare.subscription.dto.SubscriptionPlanResponse;
import com.homecare.subscription.dto.UpdateSubscriptionPlanRequest;
import com.homecare.subscription.service.SubscriptionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/subscription-plans")
@RequiredArgsConstructor
public class AdminSubscriptionPlanController {

    private final SubscriptionService subscriptionService;

    @PostMapping
    public ResponseEntity<ApiResponse<SubscriptionPlanResponse>> createPlan(
            @Valid @RequestBody CreateSubscriptionPlanRequest request) {
        SubscriptionPlanResponse plan = subscriptionService.createPlan(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(plan));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<SubscriptionPlanResponse>> updatePlan(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateSubscriptionPlanRequest request) {
        SubscriptionPlanResponse plan = subscriptionService.updatePlan(id, request);
        return ResponseEntity.ok(ApiResponse.ok(plan, "Plan updated successfully"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deactivatePlan(@PathVariable UUID id) {
        subscriptionService.deactivatePlan(id);
        return ResponseEntity.ok(ApiResponse.ok(null, "Plan deactivated successfully"));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PagedResponse<SubscriptionPlanResponse>>> listAllPlans(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        PagedResponse<SubscriptionPlanResponse> plans = subscriptionService
                .listAllPlans(PageRequest.of(page, size));
        return ResponseEntity.ok(ApiResponse.ok(plans));
    }
}

