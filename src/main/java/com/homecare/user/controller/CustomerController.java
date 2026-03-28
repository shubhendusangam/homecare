package com.homecare.user.controller;

import com.homecare.core.dto.ApiResponse;
import com.homecare.user.dto.CustomerProfileDto;
import com.homecare.user.dto.UpdateCustomerRequest;
import com.homecare.user.security.UserPrincipal;
import com.homecare.user.service.CustomerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/customers")
@RequiredArgsConstructor
public class CustomerController {

    private final CustomerService customerService;

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<CustomerProfileDto>> getProfile(
            @AuthenticationPrincipal UserPrincipal principal) {
        CustomerProfileDto profile = customerService.getProfile(principal.getId());
        return ResponseEntity.ok(ApiResponse.ok(profile));
    }

    @PutMapping("/me")
    public ResponseEntity<ApiResponse<CustomerProfileDto>> updateProfile(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody UpdateCustomerRequest request) {
        CustomerProfileDto profile = customerService.updateProfile(principal.getId(), request);
        return ResponseEntity.ok(ApiResponse.ok(profile, "Profile updated successfully"));
    }

    @GetMapping("/me/address")
    public ResponseEntity<ApiResponse<CustomerProfileDto>> getAddress(
            @AuthenticationPrincipal UserPrincipal principal) {
        CustomerProfileDto profile = customerService.getAddress(principal.getId());
        return ResponseEntity.ok(ApiResponse.ok(profile));
    }
}

