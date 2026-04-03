package com.homecare.referral.controller;

import com.homecare.core.dto.ApiResponse;
import com.homecare.core.dto.PagedResponse;
import com.homecare.referral.dto.ReferralCodeResponse;
import com.homecare.referral.dto.ReferralEventResponse;
import com.homecare.referral.dto.ReferralStatsResponse;
import com.homecare.referral.service.ReferralService;
import com.homecare.user.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/referrals")
@RequiredArgsConstructor
public class ReferralController {

    private final ReferralService referralService;

    @GetMapping("/my-code")
    public ResponseEntity<ApiResponse<ReferralCodeResponse>> getMyCode(
            @AuthenticationPrincipal UserPrincipal principal) {
        ReferralCodeResponse response = referralService.getOrCreateCode(principal.getId());
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<ReferralStatsResponse>> getStats(
            @AuthenticationPrincipal UserPrincipal principal) {
        ReferralStatsResponse response = referralService.getStats(principal.getId());
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @GetMapping("/history")
    public ResponseEntity<ApiResponse<PagedResponse<ReferralEventResponse>>> getHistory(
            @AuthenticationPrincipal UserPrincipal principal,
            @PageableDefault(size = 20) Pageable pageable) {
        PagedResponse<ReferralEventResponse> response = referralService.getHistory(principal.getId(), pageable);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }
}

