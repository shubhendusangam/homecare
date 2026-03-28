package com.homecare.payment.controller;

import com.homecare.core.dto.ApiResponse;
import com.homecare.core.dto.PagedResponse;
import com.homecare.payment.dto.PaymentSummaryResponse;
import com.homecare.payment.dto.WalletTransactionResponse;
import com.homecare.payment.enums.TransactionStatus;
import com.homecare.payment.enums.TransactionType;
import com.homecare.payment.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;

@RestController
@RequestMapping("/api/v1/admin/payments")
@RequiredArgsConstructor
public class AdminPaymentController {

    private final PaymentService paymentService;

    /**
     * GET /api/v1/admin/payments — all transactions with optional filters
     */
    @GetMapping
    public ResponseEntity<ApiResponse<PagedResponse<WalletTransactionResponse>>> getAllTransactions(
            @RequestParam(required = false) TransactionType type,
            @RequestParam(required = false) TransactionStatus status,
            @RequestParam(required = false) Instant from,
            @RequestParam(required = false) Instant to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        PagedResponse<WalletTransactionResponse> response = paymentService.getAllTransactions(
                type, status, from, to, PageRequest.of(page, size));
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    /**
     * GET /api/v1/admin/payments/summary — daily/weekly revenue summary
     */
    @GetMapping("/summary")
    public ResponseEntity<ApiResponse<PaymentSummaryResponse>> getPaymentSummary(
            @RequestParam(defaultValue = "daily") String period) {
        PaymentSummaryResponse response = paymentService.getPaymentSummary(period);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }
}

