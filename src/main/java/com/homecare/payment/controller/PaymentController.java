package com.homecare.payment.controller;

import com.homecare.core.dto.ApiResponse;
import com.homecare.payment.dto.PaymentVerifyRequest;
import com.homecare.payment.dto.TopupInitiateResponse;
import com.homecare.payment.service.PaymentService;
import com.homecare.user.security.UserPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    /**
     * POST /api/v1/payments/booking/{bookingId}/pay-wallet
     * Deducts amount from wallet, sets booking paymentStatus=PAID
     */
    @PostMapping("/booking/{bookingId}/pay-wallet")
    public ResponseEntity<ApiResponse<Void>> payBookingViaWallet(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID bookingId) {
        paymentService.payBookingViaWallet(principal.getId(), bookingId);
        return ResponseEntity.ok(ApiResponse.ok(null, "Booking paid via wallet successfully"));
    }

    /**
     * POST /api/v1/payments/booking/{bookingId}/initiate
     * Creates Razorpay order for booking payment
     */
    @PostMapping("/booking/{bookingId}/initiate")
    public ResponseEntity<ApiResponse<TopupInitiateResponse>> initiateBookingPayment(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID bookingId) {
        TopupInitiateResponse response = paymentService.initiateBookingPayment(
                principal.getId(), bookingId);
        return ResponseEntity.ok(ApiResponse.ok(response, "Payment order created"));
    }

    /**
     * POST /api/v1/payments/booking/{bookingId}/verify
     * Verifies Razorpay payment and marks booking as PAID
     */
    @PostMapping("/booking/{bookingId}/verify")
    public ResponseEntity<ApiResponse<Void>> verifyBookingPayment(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID bookingId,
            @Valid @RequestBody PaymentVerifyRequest request) {
        paymentService.verifyBookingPayment(principal.getId(), bookingId, request);
        return ResponseEntity.ok(ApiResponse.ok(null, "Payment verified successfully"));
    }

    /**
     * POST /api/v1/payments/booking/{bookingId}/refund
     * Admin or auto-refund on cancel (100% refund by default)
     */
    @PostMapping("/booking/{bookingId}/refund")
    public ResponseEntity<ApiResponse<Void>> refundBooking(
            @PathVariable UUID bookingId,
            @RequestParam(defaultValue = "1.0") BigDecimal refundPercentage) {
        paymentService.refundBooking(bookingId, refundPercentage);
        return ResponseEntity.ok(ApiResponse.ok(null, "Refund processed successfully"));
    }
}

