package com.homecare.payment.gateway;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Mock payment gateway for dev/test environments.
 * All operations succeed immediately.
 * In production, replace with a real Razorpay implementation.
 */
@Service
@Profile({"dev", "test"})
@Slf4j
public class MockPaymentGateway implements PaymentGateway {

    @Override
    public PaymentOrderResponse createOrder(BigDecimal amount, String currency, String receipt) {
        String orderId = "order_mock_" + UUID.randomUUID();
        log.info("Mock: Created order {} for {} {}", orderId, amount, currency);
        return PaymentOrderResponse.builder()
                .orderId(orderId)
                .amount(amount)
                .currency(currency)
                .status("created")
                .build();
    }

    @Override
    public boolean verifySignature(String orderId, String paymentId, String signature) {
        log.info("Mock: Verifying signature for order={}, payment={}", orderId, paymentId);
        return true; // always succeeds in dev
    }

    @Override
    public RefundResponse initiateRefund(String paymentId, BigDecimal amount) {
        String refundId = "rfnd_mock_" + UUID.randomUUID();
        log.info("Mock: Initiated refund {} for payment {} amount {}", refundId, paymentId, amount);
        return RefundResponse.builder()
                .refundId(refundId)
                .status("processed")
                .amount(amount)
                .build();
    }
}

