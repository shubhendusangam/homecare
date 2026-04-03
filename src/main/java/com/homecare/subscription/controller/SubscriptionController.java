package com.homecare.subscription.controller;

import com.homecare.core.dto.ApiResponse;
import com.homecare.core.dto.PagedResponse;
import com.homecare.subscription.dto.CancelSubscriptionRequest;
import com.homecare.subscription.dto.CustomerSubscriptionResponse;
import com.homecare.subscription.dto.SubscribeRequest;
import com.homecare.subscription.service.SubscriptionService;
import com.homecare.user.security.UserPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/subscriptions")
@RequiredArgsConstructor
public class SubscriptionController {

    private final SubscriptionService subscriptionService;

    @PostMapping
    public ResponseEntity<ApiResponse<CustomerSubscriptionResponse>> subscribe(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody SubscribeRequest request) {
        CustomerSubscriptionResponse sub = subscriptionService.subscribe(principal.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(sub));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PagedResponse<CustomerSubscriptionResponse>>> getMySubscriptions(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        PagedResponse<CustomerSubscriptionResponse> subs = subscriptionService
                .getMySubscriptions(principal.getId(), PageRequest.of(page, size));
        return ResponseEntity.ok(ApiResponse.ok(subs));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<CustomerSubscriptionResponse>> getSubscription(@PathVariable UUID id) {
        CustomerSubscriptionResponse sub = subscriptionService.getSubscription(id);
        return ResponseEntity.ok(ApiResponse.ok(sub));
    }

    @DeleteMapping("/{id}/cancel")
    public ResponseEntity<ApiResponse<CustomerSubscriptionResponse>> cancelSubscription(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id,
            @RequestBody(required = false) @Valid CancelSubscriptionRequest request) {
        CustomerSubscriptionResponse sub = subscriptionService
                .cancelSubscription(principal.getId(), id, request);
        return ResponseEntity.ok(ApiResponse.ok(sub, "Subscription cancelled successfully"));
    }
}

