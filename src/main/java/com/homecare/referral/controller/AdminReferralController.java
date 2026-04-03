package com.homecare.referral.controller;

import com.homecare.core.dto.ApiResponse;
import com.homecare.core.dto.PagedResponse;
import com.homecare.core.enums.ReferralStatus;
import com.homecare.referral.dto.AdminReferralSummaryResponse;
import com.homecare.referral.dto.ReferralEventResponse;
import com.homecare.referral.service.ReferralService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

@RestController
@RequestMapping("/api/v1/admin/referrals")
@RequiredArgsConstructor
public class AdminReferralController {

    private final ReferralService referralService;

    @GetMapping("/summary")
    public ResponseEntity<ApiResponse<AdminReferralSummaryResponse>> getSummary() {
        AdminReferralSummaryResponse response = referralService.getAdminSummary();
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PagedResponse<ReferralEventResponse>>> listEvents(
            @RequestParam(required = false) ReferralStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @PageableDefault(size = 20) Pageable pageable) {
        PagedResponse<ReferralEventResponse> response = referralService.getAdminEvents(status, from, to, pageable);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }
}

