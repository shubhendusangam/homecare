package com.homecare.payment.gateway;

import java.math.BigDecimal;

/**
 * Abstraction over payment gateways (Razorpay, Stripe, etc.).
 * In dev, {@link MockPaymentGateway} is used.
 * In prod, replace with a real Razorpay implementation.
 */
public interface PaymentGateway {

    /**
     * Creates an order on the payment gateway.
     */
    PaymentOrderResponse createOrder(BigDecimal amount, String currency, String receipt);

    /**
     * Verifies the payment signature (HMAC check).
     */
    boolean verifySignature(String orderId, String paymentId, String signature);

    /**
     * Initiates a refund for a given payment.
     */
    RefundResponse initiateRefund(String paymentId, BigDecimal amount);
}

