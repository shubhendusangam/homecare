package com.homecare.payment.controller;

import com.homecare.core.dto.ApiResponse;
import com.homecare.core.dto.PagedResponse;
import com.homecare.payment.dto.*;
import com.homecare.payment.service.PaymentService;
import com.homecare.payment.service.WalletService;
import com.homecare.user.security.UserPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/wallet")
@RequiredArgsConstructor
public class WalletController {

    private final WalletService walletService;
    private final PaymentService paymentService;

    /**
     * GET /api/v1/wallet — own wallet balance + held amount
     */
    @GetMapping
    public ResponseEntity<ApiResponse<WalletResponse>> getWallet(
            @AuthenticationPrincipal UserPrincipal principal) {
        WalletResponse response = walletService.getWalletBalance(principal.getId());
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    /**
     * GET /api/v1/wallet/transactions — paginated transaction history
     */
    @GetMapping("/transactions")
    public ResponseEntity<ApiResponse<PagedResponse<WalletTransactionResponse>>> getTransactions(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        PagedResponse<WalletTransactionResponse> response = walletService.getTransactionHistory(
                principal.getId(), PageRequest.of(page, size));
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    /**
     * POST /api/v1/wallet/topup/initiate — initiate top-up, returns Razorpay order
     */
    @PostMapping("/topup/initiate")
    public ResponseEntity<ApiResponse<TopupInitiateResponse>> initiateTopup(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody TopupInitiateRequest request) {
        TopupInitiateResponse response = paymentService.initiateTopup(
                principal.getId(), request.getAmount());
        return ResponseEntity.ok(ApiResponse.ok(response, "Top-up order created"));
    }

    /**
     * POST /api/v1/wallet/topup/verify — verify payment and credit wallet
     */
    @PostMapping("/topup/verify")
    public ResponseEntity<ApiResponse<WalletResponse>> verifyTopup(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody TopupVerifyRequest request) {
        WalletResponse response = paymentService.verifyTopup(principal.getId(), request);
        return ResponseEntity.ok(ApiResponse.ok(response, "Wallet credited successfully"));
    }
}

