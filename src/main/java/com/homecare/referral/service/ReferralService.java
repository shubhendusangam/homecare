package com.homecare.referral.service;

import com.homecare.core.dto.PagedResponse;
import com.homecare.core.enums.ErrorCode;
import com.homecare.core.enums.ReferralStatus;
import com.homecare.core.exception.BusinessException;
import com.homecare.core.exception.ResourceNotFoundException;
import com.homecare.core.logging.AuditEvent;
import com.homecare.notification.enums.NotificationType;
import com.homecare.notification.service.NotificationService;
import com.homecare.payment.entity.Wallet;
import com.homecare.payment.entity.WalletTransaction;
import com.homecare.payment.enums.TransactionStatus;
import com.homecare.payment.enums.TransactionType;
import com.homecare.payment.repository.WalletRepository;
import com.homecare.payment.repository.WalletTransactionRepository;
import com.homecare.payment.service.WalletService;
import com.homecare.referral.config.ReferralConfig;
import com.homecare.referral.dto.*;
import com.homecare.referral.entity.ReferralCode;
import com.homecare.referral.entity.ReferralEvent;
import com.homecare.referral.mapper.ReferralEventResponseMapper;
import com.homecare.referral.repository.ReferralCodeRepository;
import com.homecare.referral.repository.ReferralEventRepository;
import com.homecare.user.entity.User;
import com.homecare.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReferralService {

    private final ReferralCodeRepository referralCodeRepository;
    private final ReferralEventRepository referralEventRepository;
    private final UserRepository userRepository;
    private final WalletService walletService;
    private final WalletRepository walletRepository;
    private final WalletTransactionRepository walletTransactionRepository;
    private final NotificationService notificationService;
    private final ReferralConfig referralConfig;
    private final ApplicationEventPublisher eventPublisher;
    private final ReferralEventResponseMapper referralEventResponseMapper;

    private static final Random RANDOM = new Random();

    // ─── Get or Create Referral Code ──────────────────────────────────

    @Transactional
    public ReferralCodeResponse getOrCreateCode(UUID userId) {
        ReferralCode code = referralCodeRepository.findByUserId(userId)
                .orElseGet(() -> createReferralCode(userId));

        return ReferralCodeResponse.builder()
                .code(code.getCode())
                .totalReferrals(code.getTotalReferrals())
                .successfulReferrals(code.getSuccessfulReferrals())
                .build();
    }

    private ReferralCode createReferralCode(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        String code = generateUniqueCode(user.getName());
        ReferralCode referralCode = ReferralCode.builder()
                .user(user)
                .code(code)
                .totalReferrals(0)
                .successfulReferrals(0)
                .totalCreditsEarned(BigDecimal.ZERO)
                .build();
        referralCode = referralCodeRepository.save(referralCode);

        log.info("Referral code created: userId={}, code={}", userId, code);
        eventPublisher.publishEvent(AuditEvent.of("REFERRAL_CODE_CREATED", userId,
                Map.of("code", code)));

        return referralCode;
    }

    String generateUniqueCode(String name) {
        int maxAttempts = 10;
        for (int i = 0; i < maxAttempts; i++) {
            String code = buildCode(name, referralConfig.getCodeLength());
            if (!referralCodeRepository.existsByCode(code)) {
                return code;
            }
        }
        // Fallback: fully random code
        String fallback = "HC" + String.format("%06d", RANDOM.nextInt(999999));
        log.warn("Referral code collision, using fallback: {}", fallback);
        return fallback;
    }

    private String buildCode(String name, int length) {
        // Take first 4 chars of name (uppercase), pad if shorter
        String prefix;
        if (name != null && !name.isBlank()) {
            String cleaned = name.replaceAll("[^A-Za-z]", "").toUpperCase();
            prefix = cleaned.length() >= 4 ? cleaned.substring(0, 4) : cleaned;
        } else {
            prefix = "HOME";
        }

        int digitCount = length - prefix.length();
        if (digitCount <= 0) digitCount = 4;

        StringBuilder digits = new StringBuilder();
        for (int i = 0; i < digitCount; i++) {
            digits.append(RANDOM.nextInt(10));
        }
        return prefix + digits;
    }

    // ─── Create Referral Event (on customer registration) ─────────────

    @Transactional
    public void createEvent(User referrer, User referee, String code) {
        // Prevent self-referral
        if (referrer.getId().equals(referee.getId())) {
            throw new BusinessException("You cannot use your own referral code", ErrorCode.SELF_REFERRAL);
        }

        // Increment total referrals on the code
        ReferralCode referralCode = referralCodeRepository.findByCode(code)
                .orElseThrow(() -> new BusinessException("Invalid referral code", ErrorCode.INVALID_REFERRAL_CODE));
        referralCode.setTotalReferrals(referralCode.getTotalReferrals() + 1);
        referralCodeRepository.save(referralCode);

        // Credit referee wallet immediately
        BigDecimal refereeCredit = referralConfig.getRefereeCredit();
        creditWalletForReferral(referee.getId(), refereeCredit, "Referral signup bonus (code: " + code + ")");

        // Create event
        ReferralEvent event = ReferralEvent.builder()
                .referrer(referrer)
                .referee(referee)
                .referralCode(code)
                .status(ReferralStatus.SIGNUP_DONE)
                .signupAt(Instant.now())
                .referrerCredit(referralConfig.getReferrerCredit())
                .refereeCredit(refereeCredit)
                .referrerCreditIssued(false)
                .refereeCreditIssued(true)
                .build();
        referralEventRepository.save(event);

        // Notify referee
        notificationService.sendToUser(referee.getId(), NotificationType.REFERRAL_SIGNUP_CREDIT,
                Map.of("amount", refereeCredit.toPlainString()));

        log.info("Referral event created: referrer={}, referee={}, code={}, refereeCredit={}",
                referrer.getId(), referee.getId(), code, refereeCredit);
        eventPublisher.publishEvent(AuditEvent.of("REFERRAL_EVENT_CREATED", referee.getId(),
                Map.of("referrerId", referrer.getId(), "code", code)));
    }

    // ─── Check and Issue Referrer Credit (on first booking completion) ─

    @Transactional
    public void checkAndIssueReferrerCredit(User customer) {
        referralEventRepository.findByRefereeId(customer.getId()).ifPresent(event -> {
            // Already credited or not in SIGNUP_DONE status
            if (event.getStatus() != ReferralStatus.SIGNUP_DONE) {
                return;
            }

            // Check expiry window
            Instant expiryDeadline = event.getSignupAt()
                    .plus(referralConfig.getSignupExpiryDays(), ChronoUnit.DAYS);
            if (Instant.now().isAfter(expiryDeadline)) {
                event.setStatus(ReferralStatus.EXPIRED);
                referralEventRepository.save(event);
                log.info("Referral expired for referee={}, referrer={}", customer.getId(), event.getReferrer().getId());
                return;
            }

            // Issue referrer credit
            BigDecimal referrerCredit = event.getReferrerCredit();
            creditWalletForReferral(event.getReferrer().getId(), referrerCredit,
                    "Referral bonus — " + customer.getName() + " completed first booking");

            // Update event
            event.setStatus(ReferralStatus.CREDIT_ISSUED);
            event.setFirstBookingAt(Instant.now());
            event.setReferrerCreditIssued(true);
            referralEventRepository.save(event);

            // Update referral code stats
            referralCodeRepository.findByCode(event.getReferralCode()).ifPresent(code -> {
                code.setSuccessfulReferrals(code.getSuccessfulReferrals() + 1);
                code.setTotalCreditsEarned(code.getTotalCreditsEarned().add(referrerCredit));
                referralCodeRepository.save(code);
            });

            // Notify referrer
            notificationService.sendToUser(event.getReferrer().getId(), NotificationType.REFERRAL_BONUS_CREDIT,
                    Map.of("amount", referrerCredit.toPlainString(), "refereeName", customer.getName()));

            log.info("Referrer credit issued: referrer={}, referee={}, amount={}",
                    event.getReferrer().getId(), customer.getId(), referrerCredit);
            eventPublisher.publishEvent(AuditEvent.of("REFERRAL_CREDIT_ISSUED", event.getReferrer().getId(),
                    Map.of("refereeId", customer.getId(), "amount", referrerCredit)));
        });
    }

    // ─── Get Referral Stats ───────────────────────────────────────────

    @Transactional(readOnly = true)
    public ReferralStatsResponse getStats(UUID userId) {
        ReferralCode code = referralCodeRepository.findByUserId(userId)
                .orElse(null);
        if (code == null) {
            return ReferralStatsResponse.builder()
                    .code(null)
                    .totalReferrals(0)
                    .successfulReferrals(0)
                    .totalCreditsEarned(BigDecimal.ZERO)
                    .build();
        }

        return ReferralStatsResponse.builder()
                .code(code.getCode())
                .totalReferrals(code.getTotalReferrals())
                .successfulReferrals(code.getSuccessfulReferrals())
                .totalCreditsEarned(code.getTotalCreditsEarned())
                .build();
    }

    // ─── Get Referral History ─────────────────────────────────────────

    @Transactional(readOnly = true)
    public PagedResponse<ReferralEventResponse> getHistory(UUID userId, Pageable pageable) {
        Page<ReferralEventResponse> page = referralEventRepository
                .findByReferrerIdOrderByCreatedAtDesc(userId, pageable)
                .map(referralEventResponseMapper::toDto);
        return PagedResponse.from(page);
    }

    // ─── Admin: Summary ───────────────────────────────────────────────

    @Transactional(readOnly = true)
    public AdminReferralSummaryResponse getAdminSummary() {
        long totalSignups = referralEventRepository.count();
        long totalConversions = referralEventRepository.countByStatus(ReferralStatus.CREDIT_ISSUED);
        long totalExpired = referralEventRepository.countByStatus(ReferralStatus.EXPIRED);
        BigDecimal totalCreditsIssued = referralEventRepository.sumAllCreditsIssued();

        double conversionRate = totalSignups > 0
                ? (double) totalConversions / totalSignups * 100.0
                : 0.0;

        return AdminReferralSummaryResponse.builder()
                .totalSignups(totalSignups)
                .totalConversions(totalConversions)
                .totalExpired(totalExpired)
                .totalCreditsIssued(totalCreditsIssued)
                .conversionRate(Math.round(conversionRate * 100.0) / 100.0)
                .build();
    }

    // ─── Admin: All Events ────────────────────────────────────────────

    @Transactional(readOnly = true)
    public PagedResponse<ReferralEventResponse> getAdminEvents(ReferralStatus status,
                                                                Instant from, Instant to,
                                                                Pageable pageable) {
        Page<ReferralEventResponse> page = referralEventRepository
                .findAllWithFilters(status, from, to, pageable)
                .map(referralEventResponseMapper::toDto);
        return PagedResponse.from(page);
    }

    // ─── Private Helpers ──────────────────────────────────────────────

    private void creditWalletForReferral(UUID userId, BigDecimal amount, String description) {
        Wallet wallet = walletService.getOrCreateWallet(userId);
        wallet.setBalance(wallet.getBalance().add(amount));
        walletRepository.save(wallet);

        WalletTransaction transaction = WalletTransaction.builder()
                .wallet(wallet)
                .type(TransactionType.CREDIT_REFERRAL)
                .status(TransactionStatus.SUCCESS)
                .amount(amount)
                .description(description)
                .processedAt(Instant.now())
                .build();
        walletTransactionRepository.save(transaction);

        log.info("Referral credit: userId={}, amount={}", userId, amount);
    }
}




