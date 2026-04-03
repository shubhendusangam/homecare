package com.homecare.subscription.controller;

import com.homecare.core.dto.ApiResponse;
import com.homecare.core.dto.PagedResponse;
import com.homecare.core.enums.SubscriptionStatus;
import com.homecare.subscription.dto.CancelSubscriptionRequest;
import com.homecare.subscription.dto.CustomerSubscriptionResponse;
import com.homecare.subscription.service.SubscriptionService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/subscriptions")
@RequiredArgsConstructor
public class AdminSubscriptionController {

    private final SubscriptionService subscriptionService;

    @GetMapping
    public ResponseEntity<ApiResponse<PagedResponse<CustomerSubscriptionResponse>>> getAllSubscriptions(
            @RequestParam(required = false) SubscriptionStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        PagedResponse<CustomerSubscriptionResponse> subs = subscriptionService
                .getAllSubscriptions(status, PageRequest.of(page, size));
        return ResponseEntity.ok(ApiResponse.ok(subs));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<CustomerSubscriptionResponse>> getSubscription(@PathVariable UUID id) {
        CustomerSubscriptionResponse sub = subscriptionService.getSubscription(id);
        return ResponseEntity.ok(ApiResponse.ok(sub));
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<ApiResponse<CustomerSubscriptionResponse>> adminCancel(
            @PathVariable UUID id,
            @RequestBody(required = false) CancelSubscriptionRequest request) {
        String reason = request != null ? request.getReason() : null;
        CustomerSubscriptionResponse sub = subscriptionService.adminCancelSubscription(id, reason);
        return ResponseEntity.ok(ApiResponse.ok(sub, "Subscription cancelled by admin"));
    }
}

