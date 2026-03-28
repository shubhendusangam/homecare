package com.homecare.payment.repository;

import com.homecare.core.enums.PaymentStatus;
import com.homecare.payment.entity.PaymentOrder;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PaymentOrderRepository extends JpaRepository<PaymentOrder, UUID> {

    Optional<PaymentOrder> findByRazorpayOrderId(String razorpayOrderId);

    Optional<PaymentOrder> findByBookingIdAndStatus(UUID bookingId, PaymentStatus status);

    Optional<PaymentOrder> findByBookingIdAndRazorpayPaymentIdIsNotNull(UUID bookingId);
}

