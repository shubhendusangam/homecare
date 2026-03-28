package com.homecare.payment.service;

import com.homecare.booking.entity.Booking;
import com.homecare.core.dto.PagedResponse;
import com.homecare.core.enums.ErrorCode;
import com.homecare.core.exception.BusinessException;
import com.homecare.core.exception.ResourceNotFoundException;
import com.homecare.core.logging.AuditEvent;
import com.homecare.payment.config.PaymentConfig;
import com.homecare.payment.dto.EarningsSummaryResponse;
import com.homecare.payment.dto.WalletResponse;
import com.homecare.payment.dto.WalletTransactionResponse;
import com.homecare.payment.entity.Wallet;
import com.homecare.payment.entity.WalletTransaction;
import com.homecare.payment.enums.TransactionStatus;
import com.homecare.payment.enums.TransactionType;
import com.homecare.payment.gateway.PaymentGateway;
import com.homecare.payment.gateway.RefundResponse;
import com.homecare.payment.mapper.WalletTransactionResponseMapper;
import com.homecare.payment.repository.WalletRepository;
import com.homecare.payment.repository.WalletTransactionRepository;
import com.homecare.user.entity.User;
import com.homecare.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class WalletService {

    private final WalletRepository walletRepository;
    private final WalletTransactionRepository transactionRepository;
    private final UserRepository userRepository;
    private final PaymentConfig paymentConfig;
    private final PaymentGateway paymentGateway;
    private final ApplicationEventPublisher eventPublisher;
    private final WalletTransactionResponseMapper walletTransactionResponseMapper;

    // ─── Get or Create Wallet ─────────────────────────────────────────

    @Transactional
    public Wallet getOrCreateWallet(UUID userId) {
        return walletRepository.findByUserId(userId)
                .orElseGet(() -> {
                    User user = userRepository.findById(userId)
                            .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
                    Wallet wallet = Wallet.builder()
                            .user(user)
                            .balance(BigDecimal.ZERO)
                            .heldAmount(BigDecimal.ZERO)
                            .currency(paymentConfig.getDefaultCurrency())
                            .build();
                    return walletRepository.save(wallet);
                });
    }

    // ─── Get Wallet Balance ───────────────────────────────────────────

    @Transactional(readOnly = true)
    public WalletResponse getWalletBalance(UUID userId) {
        Wallet wallet = getOrCreateWallet(userId);
        return toWalletResponse(wallet);
    }

    // ─── Get Transaction History ──────────────────────────────────────

    @Transactional(readOnly = true)
    public PagedResponse<WalletTransactionResponse> getTransactionHistory(UUID userId, Pageable pageable) {
        Wallet wallet = getOrCreateWallet(userId);
        Page<WalletTransactionResponse> page = transactionRepository
                .findByWalletIdOrderByCreatedAtDesc(wallet.getId(), pageable)
                .map(this::toTransactionResponse);
        return PagedResponse.from(page);
    }

    // ─── Credit Wallet (top-up) ───────────────────────────────────────

    @Transactional
    public WalletResponse creditWallet(UUID userId, BigDecimal amount, String externalReference) {
        Wallet wallet = getOrCreateWallet(userId);

        wallet.setBalance(wallet.getBalance().add(amount));
        walletRepository.save(wallet);

        WalletTransaction transaction = WalletTransaction.builder()
                .wallet(wallet)
                .type(TransactionType.CREDIT_TOPUP)
                .status(TransactionStatus.SUCCESS)
                .amount(amount)
                .description("Wallet top-up")
                .externalReference(externalReference)
                .processedAt(Instant.now())
                .build();
        transactionRepository.save(transaction);

        log.info("Wallet credited: userId={}, amount={}, ref={}", userId, amount, externalReference);
        eventPublisher.publishEvent(AuditEvent.of("WALLET_TOPUP", userId,
                Map.of("amount", amount, "externalReference", externalReference)));

        return toWalletResponse(wallet);
    }

    // ─── Hold Amount (booking creation) ───────────────────────────────

    @Transactional
    public void holdAmount(UUID userId, UUID bookingId, BigDecimal amount, Booking booking) {
        Wallet wallet = getOrCreateWallet(userId);

        // Check if balance minus currently held amount is enough for the new hold
        BigDecimal newHeldAmount = wallet.getHeldAmount().add(amount);
        if (wallet.getBalance().compareTo(newHeldAmount) < 0) {
            throw new BusinessException(
                    "Insufficient wallet balance. Available: ₹" + wallet.getAvailableBalance()
                            + ", Required: ₹" + amount,
                    ErrorCode.INSUFFICIENT_WALLET_BALANCE);
        }

        wallet.setHeldAmount(newHeldAmount);
        walletRepository.save(wallet);

        WalletTransaction transaction = WalletTransaction.builder()
                .wallet(wallet)
                .booking(booking)
                .type(TransactionType.DEBIT_BOOKING)
                .status(TransactionStatus.PENDING)
                .amount(amount)
                .description("Hold for booking " + bookingId)
                .build();
        transactionRepository.save(transaction);

        log.info("Amount held: userId={}, bookingId={}, amount={}", userId, bookingId, amount);
        eventPublisher.publishEvent(AuditEvent.of("WALLET_HOLD", userId,
                Map.of("bookingId", bookingId, "amount", amount)));
    }

    // ─── Release Hold (booking completed) ─────────────────────────────

    @Transactional
    public void releaseHold(UUID bookingId, Booking booking) {
        UUID customerId = booking.getCustomer().getId();
        BigDecimal totalPrice = BigDecimal.valueOf(booking.getTotalPrice());
        Wallet customerWallet = getOrCreateWallet(customerId);

        // Finalize deduction from customer wallet
        customerWallet.setBalance(customerWallet.getBalance().subtract(totalPrice));
        customerWallet.setHeldAmount(customerWallet.getHeldAmount().subtract(totalPrice));
        walletRepository.save(customerWallet);

        // Mark the DEBIT_BOOKING transaction as SUCCESS
        transactionRepository.findByBookingIdAndTypeAndStatus(
                bookingId, TransactionType.DEBIT_BOOKING, TransactionStatus.PENDING
        ).ifPresent(tx -> {
            tx.setStatus(TransactionStatus.SUCCESS);
            tx.setProcessedAt(Instant.now());
            transactionRepository.save(tx);
        });

        // Credit helper wallet (85% of booking amount — 15% platform fee)
        if (booking.getHelper() != null) {
            UUID helperId = booking.getHelper().getId();
            BigDecimal platformFee = paymentConfig.getPlatformFee();
            BigDecimal helperShare = totalPrice.multiply(BigDecimal.ONE.subtract(platformFee))
                    .setScale(2, RoundingMode.HALF_UP);

            Wallet helperWallet = getOrCreateWallet(helperId);
            helperWallet.setBalance(helperWallet.getBalance().add(helperShare));
            walletRepository.save(helperWallet);

            WalletTransaction helperTx = WalletTransaction.builder()
                    .wallet(helperWallet)
                    .booking(booking)
                    .type(TransactionType.CREDIT_EARNING)
                    .status(TransactionStatus.SUCCESS)
                    .amount(helperShare)
                    .description("Earning for booking " + bookingId + " (after " +
                            platformFee.multiply(BigDecimal.valueOf(100)).intValue() + "% platform fee)")
                    .processedAt(Instant.now())
                    .build();
            transactionRepository.save(helperTx);

            log.info("Helper credited: helperId={}, bookingId={}, amount={}", helperId, bookingId, helperShare);
            eventPublisher.publishEvent(AuditEvent.of("HELPER_EARNING_CREDITED", helperId,
                    Map.of("bookingId", bookingId, "amount", helperShare)));
        }

        log.info("Hold released: bookingId={}, amount={}", bookingId, totalPrice);
        eventPublisher.publishEvent(AuditEvent.of("WALLET_HOLD_RELEASED", customerId,
                Map.of("bookingId", bookingId, "amount", totalPrice)));
    }

    // ─── Refund (booking cancelled) ───────────────────────────────────

    @Transactional
    public void refund(UUID bookingId, Booking booking, BigDecimal refundPercentage) {
        UUID customerId = booking.getCustomer().getId();
        BigDecimal totalPrice = BigDecimal.valueOf(booking.getTotalPrice());
        BigDecimal refundAmount = totalPrice.multiply(refundPercentage).setScale(2, RoundingMode.HALF_UP);

        Wallet customerWallet = getOrCreateWallet(customerId);

        // Check if a wallet hold exists (DEBIT_BOOKING in PENDING status)
        var pendingHold = transactionRepository.findByBookingIdAndTypeAndStatus(
                bookingId, TransactionType.DEBIT_BOOKING, TransactionStatus.PENDING);

        if (pendingHold.isPresent()) {
            // Wallet-paid: release hold and adjust balance
            // balance stays the same for the held portion, we only lose the non-refunded part
            BigDecimal newBalance = customerWallet.getBalance().subtract(totalPrice).add(refundAmount);
            BigDecimal newHeldAmount = customerWallet.getHeldAmount().subtract(totalPrice);
            // Guard against negative balance/held amount
            if (newBalance.compareTo(BigDecimal.ZERO) < 0) {
                log.warn("Refund would result in negative balance for userId={}, capping at 0", customerId);
                newBalance = BigDecimal.ZERO;
            }
            if (newHeldAmount.compareTo(BigDecimal.ZERO) < 0) {
                newHeldAmount = BigDecimal.ZERO;
            }
            customerWallet.setBalance(newBalance);
            customerWallet.setHeldAmount(newHeldAmount);

            // Mark existing DEBIT_BOOKING as FAILED (it was cancelled, not completed)
            WalletTransaction holdTx = pendingHold.get();
            holdTx.setStatus(TransactionStatus.FAILED);
            holdTx.setProcessedAt(Instant.now());
            transactionRepository.save(holdTx);
        } else {
            // Razorpay-paid (no wallet hold): just credit the refund amount to wallet
            customerWallet.setBalance(customerWallet.getBalance().add(refundAmount));
        }
        walletRepository.save(customerWallet);

        // Create REFUND transaction
        WalletTransaction refundTx = WalletTransaction.builder()
                .wallet(customerWallet)
                .booking(booking)
                .type(TransactionType.REFUND)
                .status(TransactionStatus.SUCCESS)
                .amount(refundAmount)
                .description("Refund for booking " + bookingId +
                        " (" + refundPercentage.multiply(BigDecimal.valueOf(100)).intValue() + "% refund)")
                .processedAt(Instant.now())
                .build();

        // If paid via Razorpay, initiate gateway refund too
        if (booking.getPaymentReference() != null && !booking.getPaymentReference().isBlank()
                && !booking.getPaymentReference().startsWith("wallet_")) {
            RefundResponse refundResponse = paymentGateway.initiateRefund(
                    booking.getPaymentReference(), refundAmount);
            refundTx.setExternalReference(refundResponse.getRefundId());
        }

        transactionRepository.save(refundTx);

        log.info("Refund processed: bookingId={}, refundAmount={}, percentage={}%",
                bookingId, refundAmount, refundPercentage.multiply(BigDecimal.valueOf(100)));
        eventPublisher.publishEvent(AuditEvent.of("WALLET_REFUND", customerId,
                Map.of("bookingId", bookingId, "refundAmount", refundAmount)));
    }

    // ─── Pay Booking via Wallet (direct deduction) ────────────────────

    @Transactional
    public void payBookingViaWallet(UUID userId, Booking booking) {
        BigDecimal amount = BigDecimal.valueOf(booking.getTotalPrice());
        Wallet wallet = getOrCreateWallet(userId);

        if (wallet.getAvailableBalance().compareTo(amount) < 0) {
            throw new BusinessException(
                    "Insufficient wallet balance. Available: ₹" + wallet.getAvailableBalance()
                            + ", Required: ₹" + amount,
                    ErrorCode.INSUFFICIENT_WALLET_BALANCE);
        }

        // Deduct and hold (will be released on completion)
        wallet.setHeldAmount(wallet.getHeldAmount().add(amount));
        walletRepository.save(wallet);

        WalletTransaction transaction = WalletTransaction.builder()
                .wallet(wallet)
                .booking(booking)
                .type(TransactionType.DEBIT_BOOKING)
                .status(TransactionStatus.PENDING)
                .amount(amount)
                .description("Wallet payment for booking " + booking.getId())
                .build();
        transactionRepository.save(transaction);

        log.info("Booking paid via wallet: userId={}, bookingId={}, amount={}", userId, booking.getId(), amount);
    }

    // ─── Helper Earnings Summary ──────────────────────────────────────

    @Transactional(readOnly = true)
    public EarningsSummaryResponse getHelperEarnings(UUID helperId) {
        Wallet wallet = getOrCreateWallet(helperId);

        BigDecimal totalEarnings = transactionRepository.sumAmountByUserIdAndType(
                helperId, TransactionType.CREDIT_EARNING);

        long completedJobs = transactionRepository.countCompletedJobsByUserId(helperId);

        Page<WalletTransaction> recentPage = transactionRepository.findByWalletIdOrderByCreatedAtDesc(
                wallet.getId(), PageRequest.of(0, 10));

        return EarningsSummaryResponse.builder()
                .totalEarnings(totalEarnings)
                .availableBalance(wallet.getAvailableBalance())
                .currency(wallet.getCurrency())
                .totalCompletedJobs(completedJobs)
                .recentTransactions(recentPage.getContent().stream()
                        .map(this::toTransactionResponse).toList())
                .build();
    }

    // ─── Private Helpers ──────────────────────────────────────────────


    private WalletResponse toWalletResponse(Wallet wallet) {
        return WalletResponse.builder()
                .walletId(wallet.getId())
                .userId(wallet.getUser().getId())
                .balance(wallet.getBalance())
                .heldAmount(wallet.getHeldAmount())
                .availableBalance(wallet.getAvailableBalance())
                .currency(wallet.getCurrency())
                .build();
    }

    private WalletTransactionResponse toTransactionResponse(WalletTransaction tx) {
        return walletTransactionResponseMapper.toDto(tx);
    }
}

