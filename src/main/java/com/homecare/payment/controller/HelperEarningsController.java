package com.homecare.payment.controller;

import com.homecare.core.dto.ApiResponse;
import com.homecare.payment.dto.EarningsSummaryResponse;
import com.homecare.payment.service.WalletService;
import com.homecare.user.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/helpers/earnings")
@RequiredArgsConstructor
public class HelperEarningsController {

    private final WalletService walletService;

    /**
     * GET /api/v1/helpers/earnings — summary + transaction list
     */
    @GetMapping
    public ResponseEntity<ApiResponse<EarningsSummaryResponse>> getEarnings(
            @AuthenticationPrincipal UserPrincipal principal) {
        EarningsSummaryResponse response = walletService.getHelperEarnings(principal.getId());
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    /**
     * GET /api/v1/helpers/earnings/withdraw — initiate payout (stub for bank transfer)
     */
    @GetMapping("/withdraw")
    public ResponseEntity<ApiResponse<String>> initiateWithdraw(
            @AuthenticationPrincipal UserPrincipal principal) {
        // Stub — in production, this would trigger a bank transfer
        return ResponseEntity.ok(ApiResponse.ok(
                "Withdrawal request submitted. Payout will be processed within 2-3 business days.",
                "Withdrawal initiated"));
    }
}

