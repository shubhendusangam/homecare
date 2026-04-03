package com.homecare.user.controller;

import com.homecare.core.dto.ApiResponse;
import com.homecare.user.dto.*;
import com.homecare.user.security.UserPrincipal;
import com.homecare.user.service.HelperAvailabilityService;
import com.homecare.user.service.HelperService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/helpers")
@RequiredArgsConstructor
public class HelperController {

    private final HelperService helperService;
    private final HelperAvailabilityService helperAvailabilityService;

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

    // ─── Availability schedule ────────────────────────────────────────

    @GetMapping("/me/availability")
    public ResponseEntity<ApiResponse<List<AvailabilitySlotResponse>>> getMyAvailability(
            @AuthenticationPrincipal UserPrincipal principal) {
        List<AvailabilitySlotResponse> slots = helperAvailabilityService.getWeeklySchedule(principal.getId());
        return ResponseEntity.ok(ApiResponse.ok(slots));
    }

    @PutMapping("/me/availability")
    public ResponseEntity<ApiResponse<List<AvailabilitySlotResponse>>> setMyAvailability(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody List<AvailabilitySlotRequest> request) {
        List<AvailabilitySlotResponse> slots = helperAvailabilityService
                .setWeeklySchedule(principal.getId(), request);
        return ResponseEntity.ok(ApiResponse.ok(slots, "Weekly schedule updated"));
    }

    @PostMapping("/me/unavailable-dates")
    public ResponseEntity<ApiResponse<UnavailableDateResponse>> markDateUnavailable(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody UnavailableDateRequest request) {
        UnavailableDateResponse response = helperAvailabilityService
                .markDateUnavailable(principal.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(response));
    }

    @GetMapping("/me/unavailable-dates")
    public ResponseEntity<ApiResponse<List<UnavailableDateResponse>>> getMyUnavailableDates(
            @AuthenticationPrincipal UserPrincipal principal) {
        List<UnavailableDateResponse> dates = helperAvailabilityService.getUnavailableDates(principal.getId());
        return ResponseEntity.ok(ApiResponse.ok(dates));
    }

    @DeleteMapping("/me/unavailable-dates/{id}")
    public ResponseEntity<ApiResponse<Void>> removeDateUnavailability(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id) {
        helperAvailabilityService.removeDateUnavailability(principal.getId(), id);
        return ResponseEntity.ok(ApiResponse.ok(null, "Unavailable date removed"));
    }

    // ─── Public helper availability (any authenticated user) ──────────

    @GetMapping("/{helperId}/availability")
    public ResponseEntity<ApiResponse<List<AvailabilitySlotResponse>>> getHelperAvailability(
            @PathVariable UUID helperId) {
        List<AvailabilitySlotResponse> slots = helperAvailabilityService.getWeeklySchedule(helperId);
        return ResponseEntity.ok(ApiResponse.ok(slots));
    }
}

