package com.homecare.payment.service;

import com.homecare.booking.entity.Booking;
import com.homecare.booking.repository.BookingRepository;
import com.homecare.core.dto.PagedResponse;
import com.homecare.core.enums.ErrorCode;
import com.homecare.core.enums.PaymentStatus;
import com.homecare.core.exception.BusinessException;
import com.homecare.core.exception.ResourceNotFoundException;
import com.homecare.core.logging.AuditEvent;
import com.homecare.payment.config.PaymentConfig;
import com.homecare.payment.dto.*;
import com.homecare.payment.entity.PaymentOrder;
import com.homecare.payment.enums.TransactionStatus;
import com.homecare.payment.enums.TransactionType;
import com.homecare.payment.gateway.PaymentGateway;
import com.homecare.payment.gateway.PaymentOrderResponse;
import com.homecare.payment.mapper.WalletTransactionResponseMapper;
import com.homecare.payment.repository.PaymentOrderRepository;
import com.homecare.payment.repository.WalletTransactionRepository;
import com.homecare.user.entity.User;
import com.homecare.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    private final PaymentOrderRepository paymentOrderRepository;
    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;
    private final WalletService walletService;
    private final PaymentGateway paymentGateway;
    private final PaymentConfig paymentConfig;
    private final WalletTransactionRepository transactionRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final WalletTransactionResponseMapper walletTransactionResponseMapper;

    // ─── Top-up: Initiate ─────────────────────────────────────────────

    @Transactional
    public TopupInitiateResponse initiateTopup(UUID userId, BigDecimal amount) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        PaymentOrderResponse gatewayResponse = paymentGateway.createOrder(
                amount, paymentConfig.getDefaultCurrency(), "topup_" + userId);

        PaymentOrder order = PaymentOrder.builder()
                .user(user)
                .amount(amount)
                .razorpayOrderId(gatewayResponse.getOrderId())
                .status(PaymentStatus.PENDING)
                .build();
        paymentOrderRepository.save(order);

        log.info("Topup initiated: userId={}, amount={}, orderId={}", userId, amount, gatewayResponse.getOrderId());

        return TopupInitiateResponse.builder()
                .razorpayOrderId(gatewayResponse.getOrderId())
                .amount(amount)
                .currency(paymentConfig.getDefaultCurrency())
                .key(paymentConfig.getRazorpayKeyId())
                .build();
    }

    // ─── Top-up: Verify ───────────────────────────────────────────────

    @Transactional
    public WalletResponse verifyTopup(UUID userId, TopupVerifyRequest request) {
        PaymentOrder order = paymentOrderRepository.findByRazorpayOrderId(request.getRazorpayOrderId())
                .orElseThrow(() -> new ResourceNotFoundException("PaymentOrder", "razorpayOrderId",
                        request.getRazorpayOrderId()));

        // Ensure the order belongs to this user
        if (!order.getUser().getId().equals(userId)) {
            throw new BusinessException("Payment order does not belong to this user", ErrorCode.FORBIDDEN);
        }

        if (order.getStatus() == PaymentStatus.PAID) {
            throw new BusinessException("Payment already verified", ErrorCode.VALIDATION_FAILED);
        }

        // Verify signature via gateway
        boolean valid = paymentGateway.verifySignature(
                request.getRazorpayOrderId(),
                request.getRazorpayPaymentId(),
                request.getRazorpaySignature());

        if (!valid) {
            order.setStatus(PaymentStatus.FAILED);
            order.setFailureReason("Signature verification failed");
            paymentOrderRepository.save(order);
            throw new BusinessException("Payment verification failed", ErrorCode.PAYMENT_FAILED);
        }

        order.setRazorpayPaymentId(request.getRazorpayPaymentId());
        order.setRazorpaySignature(request.getRazorpaySignature());
        order.setStatus(PaymentStatus.PAID);
        paymentOrderRepository.save(order);

        // Credit wallet
        WalletResponse walletResponse = walletService.creditWallet(
                userId, order.getAmount(), request.getRazorpayPaymentId());

        log.info("Topup verified: userId={}, amount={}", userId, order.getAmount());
        eventPublisher.publishEvent(AuditEvent.of("TOPUP_VERIFIED", userId,
                Map.of("amount", order.getAmount(), "paymentId", request.getRazorpayPaymentId())));

        return walletResponse;
    }

    // ─── Booking Payment via Wallet ───────────────────────────────────

    @Transactional
    public void payBookingViaWallet(UUID userId, UUID bookingId) {
        Booking booking = findBooking(bookingId);

        if (!booking.getCustomer().getId().equals(userId)) {
            throw new BusinessException("You can only pay for your own bookings", ErrorCode.FORBIDDEN);
        }

        if (booking.getPaymentStatus() == PaymentStatus.PAID
                || booking.getPaymentStatus() == PaymentStatus.HELD) {
            throw new BusinessException("Booking is already paid", ErrorCode.VALIDATION_FAILED);
        }

        walletService.payBookingViaWallet(userId, booking);
        booking.setPaymentStatus(PaymentStatus.HELD);
        booking.setPaymentReference("wallet_" + userId);
        bookingRepository.save(booking);

        log.info("Booking paid via wallet: userId={}, bookingId={}", userId, bookingId);
        eventPublisher.publishEvent(AuditEvent.of("BOOKING_PAID_WALLET", userId,
                Map.of("bookingId", bookingId, "amount", booking.getTotalPrice())));
    }

    // ─── Booking Payment via Razorpay: Initiate ───────────────────────

    @Transactional
    public TopupInitiateResponse initiateBookingPayment(UUID userId, UUID bookingId) {
        Booking booking = findBooking(bookingId);

        if (!booking.getCustomer().getId().equals(userId)) {
            throw new BusinessException("You can only pay for your own bookings", ErrorCode.FORBIDDEN);
        }

        if (booking.getPaymentStatus() == PaymentStatus.PAID
                || booking.getPaymentStatus() == PaymentStatus.HELD) {
            throw new BusinessException("Booking is already paid", ErrorCode.VALIDATION_FAILED);
        }

        BigDecimal amount = BigDecimal.valueOf(booking.getTotalPrice());
        PaymentOrderResponse gatewayResponse = paymentGateway.createOrder(
                amount, paymentConfig.getDefaultCurrency(), "booking_" + bookingId);

        PaymentOrder order = PaymentOrder.builder()
                .user(booking.getCustomer())
                .booking(booking)
                .amount(amount)
                .razorpayOrderId(gatewayResponse.getOrderId())
                .status(PaymentStatus.PENDING)
                .build();
        paymentOrderRepository.save(order);

        log.info("Booking payment initiated: bookingId={}, orderId={}", bookingId, gatewayResponse.getOrderId());

        return TopupInitiateResponse.builder()
                .razorpayOrderId(gatewayResponse.getOrderId())
                .amount(amount)
                .currency(paymentConfig.getDefaultCurrency())
                .key(paymentConfig.getRazorpayKeyId())
                .build();
    }

    // ─── Booking Payment via Razorpay: Verify ─────────────────────────

    @Transactional
    public void verifyBookingPayment(UUID userId, UUID bookingId, PaymentVerifyRequest request) {
        Booking booking = findBooking(bookingId);

        if (!booking.getCustomer().getId().equals(userId)) {
            throw new BusinessException("You can only verify payment for your own bookings", ErrorCode.FORBIDDEN);
        }

        PaymentOrder order = paymentOrderRepository.findByRazorpayOrderId(request.getRazorpayOrderId())
                .orElseThrow(() -> new ResourceNotFoundException("PaymentOrder", "razorpayOrderId",
                        request.getRazorpayOrderId()));

        boolean valid = paymentGateway.verifySignature(
                request.getRazorpayOrderId(),
                request.getRazorpayPaymentId(),
                request.getRazorpaySignature());

        if (!valid) {
            order.setStatus(PaymentStatus.FAILED);
            order.setFailureReason("Signature verification failed");
            paymentOrderRepository.save(order);

            booking.setPaymentStatus(PaymentStatus.FAILED);
            bookingRepository.save(booking);

            throw new BusinessException("Payment verification failed", ErrorCode.PAYMENT_FAILED);
        }

        order.setRazorpayPaymentId(request.getRazorpayPaymentId());
        order.setRazorpaySignature(request.getRazorpaySignature());
        order.setStatus(PaymentStatus.PAID);
        paymentOrderRepository.save(order);

        booking.setPaymentStatus(PaymentStatus.PAID);
        booking.setPaymentReference(request.getRazorpayPaymentId());
        bookingRepository.save(booking);

        log.info("Booking payment verified: bookingId={}, paymentId={}", bookingId, request.getRazorpayPaymentId());
        eventPublisher.publishEvent(AuditEvent.of("BOOKING_PAID_RAZORPAY", userId,
                Map.of("bookingId", bookingId, "paymentId", request.getRazorpayPaymentId())));
    }

    // ─── Release Booking Payment (on completion) ───────────────────

    @Transactional
    public void releaseBookingPayment(UUID bookingId) {
        Booking booking = findBooking(bookingId);
        walletService.releaseHold(bookingId, booking);
        log.info("Booking payment released: bookingId={}", bookingId);
    }

    // ─── Refund Booking ───────────────────────────────────────────────

    @Transactional
    public void refundBooking(UUID bookingId, BigDecimal refundPercentage) {
        Booking booking = findBooking(bookingId);

        if (booking.getPaymentStatus() == PaymentStatus.REFUNDED) {
            throw new BusinessException("Booking is already refunded", ErrorCode.VALIDATION_FAILED);
        }

        walletService.refund(bookingId, booking, refundPercentage);

        booking.setPaymentStatus(PaymentStatus.REFUNDED);
        bookingRepository.save(booking);

        log.info("Booking refunded: bookingId={}, percentage={}%",
                bookingId, refundPercentage.multiply(BigDecimal.valueOf(100)));
    }

    // ─── Admin: All Transactions ──────────────────────────────────────

    @Transactional(readOnly = true)
    public PagedResponse<WalletTransactionResponse> getAllTransactions(
            TransactionType type, TransactionStatus status,
            Instant from, Instant to, Pageable pageable) {
        var page = transactionRepository.findAllWithFilters(type, status, from, to, pageable)
                .map(this::toTransactionResponse);
        return PagedResponse.from(page);
    }

    // ─── Admin: Payment Summary ───────────────────────────────────────

    @Transactional(readOnly = true)
    public PaymentSummaryResponse getPaymentSummary(String period) {
        Instant now = Instant.now();
        Instant from;

        if ("weekly".equalsIgnoreCase(period)) {
            from = now.minus(7, ChronoUnit.DAYS);
        } else {
            from = now.truncatedTo(ChronoUnit.DAYS);
            period = "daily";
        }

        BigDecimal totalRevenue = transactionRepository.sumRevenueInPeriod(from, now);
        long totalTransactions = transactionRepository.countTransactionsInPeriod(from, now);

        // Sum refunds in the same period (approximation)
        BigDecimal totalRefunds = BigDecimal.ZERO; // Could add another query if needed

        return PaymentSummaryResponse.builder()
                .totalRevenue(totalRevenue)
                .totalTransactions(totalTransactions)
                .totalRefunds(totalRefunds)
                .period(period)
                .build();
    }

    // ─── Private Helpers ──────────────────────────────────────────────

    private Booking findBooking(UUID bookingId) {
        return bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking", "id", bookingId));
    }

    private WalletTransactionResponse toTransactionResponse(com.homecare.payment.entity.WalletTransaction tx) {
        return walletTransactionResponseMapper.toDto(tx);
    }
}

