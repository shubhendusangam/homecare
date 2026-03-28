package com.homecare.user.controller;

import com.homecare.core.dto.ApiResponse;
import com.homecare.user.dto.*;
import com.homecare.user.security.UserPrincipal;
import com.homecare.user.service.HelperService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/helpers")
@RequiredArgsConstructor
public class HelperController {

    private final HelperService helperService;

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<HelperProfileDto>> getProfile(
            @AuthenticationPrincipal UserPrincipal principal) {
        HelperProfileDto profile = helperService.getProfile(principal.getId());
        return ResponseEntity.ok(ApiResponse.ok(profile));
    }

    @PutMapping("/me")
    public ResponseEntity<ApiResponse<HelperProfileDto>> updateProfile(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody UpdateHelperRequest request) {
        HelperProfileDto profile = helperService.updateProfile(principal.getId(), request);
        return ResponseEntity.ok(ApiResponse.ok(profile, "Profile updated successfully"));
    }

    @PatchMapping("/me/status")
    public ResponseEntity<ApiResponse<HelperProfileDto>> updateStatus(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody HelperStatusRequest request) {
        HelperProfileDto profile = helperService.updateStatus(principal.getId(), request);
        return ResponseEntity.ok(ApiResponse.ok(profile, "Status updated"));
    }

    @PatchMapping("/me/location")
    public ResponseEntity<ApiResponse<Void>> updateLocation(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody LocationUpdateRequest request) {
        helperService.updateLocation(principal.getId(), request);
        return ResponseEntity.ok(ApiResponse.ok(null, "Location updated"));
    }
}

