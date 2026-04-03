package com.homecare.referral.service;

import com.homecare.core.enums.ErrorCode;
import com.homecare.core.enums.ReferralStatus;
import com.homecare.core.exception.BusinessException;
import com.homecare.core.exception.ResourceNotFoundException;
import com.homecare.core.logging.AuditEvent;
import com.homecare.notification.enums.NotificationType;
import com.homecare.notification.service.NotificationService;
import com.homecare.payment.entity.Wallet;
import com.homecare.payment.repository.WalletRepository;
import com.homecare.payment.repository.WalletTransactionRepository;
import com.homecare.payment.service.WalletService;
import com.homecare.referral.config.ReferralConfig;
import com.homecare.referral.dto.ReferralCodeResponse;
import com.homecare.referral.dto.ReferralStatsResponse;
import com.homecare.referral.entity.ReferralCode;
import com.homecare.referral.entity.ReferralEvent;
import com.homecare.referral.mapper.ReferralEventResponseMapper;
import com.homecare.referral.repository.ReferralCodeRepository;
import com.homecare.referral.repository.ReferralEventRepository;
import com.homecare.user.entity.User;
import com.homecare.user.enums.Role;
import com.homecare.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ReferralService")
class ReferralServiceTest {

    @Mock private ReferralCodeRepository referralCodeRepository;
    @Mock private ReferralEventRepository referralEventRepository;
    @Mock private UserRepository userRepository;
    @Mock private WalletService walletService;
    @Mock private WalletRepository walletRepository;
    @Mock private WalletTransactionRepository walletTransactionRepository;
    @Mock private NotificationService notificationService;
    @Mock private ReferralConfig referralConfig;
    @Mock private ApplicationEventPublisher eventPublisher;
    @Mock private ReferralEventResponseMapper referralEventResponseMapper;

    @InjectMocks private ReferralService referralService;

    private User referrer;
    private User referee;
    private ReferralCode referralCode;
    private Wallet referrerWallet;
    private Wallet refereeWallet;

    @BeforeEach
    void setUp() {
        referrer = User.builder().name("Priya").email("priya@test.com").role(Role.CUSTOMER).build();
        referrer.setId(UUID.randomUUID());

        referee = User.builder().name("Amit").email("amit@test.com").role(Role.CUSTOMER).build();
        referee.setId(UUID.randomUUID());

        referralCode = ReferralCode.builder()
                .user(referrer)
                .code("PRIY4821")
                .totalReferrals(0)
                .successfulReferrals(0)
                .totalCreditsEarned(BigDecimal.ZERO)
                .build();
        referralCode.setId(UUID.randomUUID());

        referrerWallet = Wallet.builder()
                .user(referrer)
                .balance(new BigDecimal("500.00"))
                .heldAmount(BigDecimal.ZERO)
                .currency("INR")
                .build();
        referrerWallet.setId(UUID.randomUUID());

        refereeWallet = Wallet.builder()
                .user(referee)
                .balance(BigDecimal.ZERO)
                .heldAmount(BigDecimal.ZERO)
                .currency("INR")
                .build();
        refereeWallet.setId(UUID.randomUUID());
    }

    // ─── getOrCreateCode ──────────────────────────────────────────────

    @Nested
    @DisplayName("getOrCreateCode")
    class GetOrCreateCode {

        @Test
        @DisplayName("creates unique code on first call")
        void createsCodeOnFirstCall() {
            when(referralCodeRepository.findByUserId(referrer.getId())).thenReturn(Optional.empty());
            when(userRepository.findById(referrer.getId())).thenReturn(Optional.of(referrer));
            when(referralConfig.getCodeLength()).thenReturn(8);
            when(referralCodeRepository.existsByCode(anyString())).thenReturn(false);
            when(referralCodeRepository.save(any(ReferralCode.class))).thenAnswer(inv -> {
                ReferralCode saved = inv.getArgument(0);
                saved.setId(UUID.randomUUID());
                return saved;
            });

            ReferralCodeResponse response = referralService.getOrCreateCode(referrer.getId());

            assertNotNull(response.getCode());
            assertTrue(response.getCode().startsWith("PRIY"));
            assertEquals(8, response.getCode().length());
            assertEquals(0, response.getTotalReferrals());
            verify(referralCodeRepository).save(any(ReferralCode.class));
        }

        @Test
        @DisplayName("second call returns same code")
        void returnsSameCodeOnSecondCall() {
            when(referralCodeRepository.findByUserId(referrer.getId())).thenReturn(Optional.of(referralCode));

            ReferralCodeResponse response = referralService.getOrCreateCode(referrer.getId());

            assertEquals("PRIY4821", response.getCode());
            verify(referralCodeRepository, never()).save(any());
        }

        @Test
        @DisplayName("user not found → ResourceNotFoundException")
        void userNotFound() {
            when(referralCodeRepository.findByUserId(referrer.getId())).thenReturn(Optional.empty());
            when(userRepository.findById(referrer.getId())).thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class,
                    () -> referralService.getOrCreateCode(referrer.getId()));
        }
    }

    // ─── createEvent ──────────────────────────────────────────────────

    @Nested
    @DisplayName("createEvent")
    class CreateEvent {

        @Test
        @DisplayName("valid code creates ReferralEvent and credits referee")
        void validCodeCreatesEventAndCreditsReferee() {
            when(referralCodeRepository.findByCode("PRIY4821")).thenReturn(Optional.of(referralCode));
            when(referralConfig.getRefereeCredit()).thenReturn(new BigDecimal("50.00"));
            when(referralConfig.getReferrerCredit()).thenReturn(new BigDecimal("100.00"));
            when(walletService.getOrCreateWallet(referee.getId())).thenReturn(refereeWallet);
            when(walletTransactionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(referralEventRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(referralCodeRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            referralService.createEvent(referrer, referee, "PRIY4821");

            // Verify event created
            ArgumentCaptor<ReferralEvent> eventCaptor = ArgumentCaptor.forClass(ReferralEvent.class);
            verify(referralEventRepository).save(eventCaptor.capture());
            ReferralEvent saved = eventCaptor.getValue();
            assertEquals(ReferralStatus.SIGNUP_DONE, saved.getStatus());
            assertEquals(referrer.getId(), saved.getReferrer().getId());
            assertEquals(referee.getId(), saved.getReferee().getId());
            assertTrue(saved.isRefereeCreditIssued());
            assertFalse(saved.isReferrerCreditIssued());

            // Verify referee wallet credited
            assertEquals(new BigDecimal("50.00"), refereeWallet.getBalance());

            // Verify referral code counter incremented
            assertEquals(1, referralCode.getTotalReferrals());

            // Verify notification sent to referee
            verify(notificationService).sendToUser(eq(referee.getId()),
                    eq(NotificationType.REFERRAL_SIGNUP_CREDIT), anyMap());
        }

        @Test
        @DisplayName("self-referral → BusinessException SELF_REFERRAL")
        void selfReferral() {
            BusinessException ex = assertThrows(BusinessException.class,
                    () -> referralService.createEvent(referrer, referrer, "PRIY4821"));
            assertEquals(ErrorCode.SELF_REFERRAL, ex.getErrorCode());
        }

        @Test
        @DisplayName("invalid code → BusinessException INVALID_REFERRAL_CODE")
        void invalidCode() {
            when(referralCodeRepository.findByCode("INVALID")).thenReturn(Optional.empty());

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> referralService.createEvent(referrer, referee, "INVALID"));
            assertEquals(ErrorCode.INVALID_REFERRAL_CODE, ex.getErrorCode());
        }
    }

    // ─── checkAndIssueReferrerCredit ──────────────────────────────────

    @Nested
    @DisplayName("checkAndIssueReferrerCredit")
    class CheckAndIssueReferrerCredit {

        private ReferralEvent referralEvent;

        @BeforeEach
        void setUpEvent() {
            referralEvent = ReferralEvent.builder()
                    .referrer(referrer)
                    .referee(referee)
                    .referralCode("PRIY4821")
                    .status(ReferralStatus.SIGNUP_DONE)
                    .signupAt(Instant.now().minus(5, ChronoUnit.DAYS))
                    .referrerCredit(new BigDecimal("100.00"))
                    .refereeCredit(new BigDecimal("50.00"))
                    .referrerCreditIssued(false)
                    .refereeCreditIssued(true)
                    .build();
            referralEvent.setId(UUID.randomUUID());
        }

        @Test
        @DisplayName("first booking completion credits referrer and marks CREDIT_ISSUED")
        void firstBookingCreditsReferrer() {
            when(referralEventRepository.findByRefereeId(referee.getId())).thenReturn(Optional.of(referralEvent));
            when(referralConfig.getSignupExpiryDays()).thenReturn(30);
            when(walletService.getOrCreateWallet(referrer.getId())).thenReturn(referrerWallet);
            when(walletTransactionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(referralEventRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(referralCodeRepository.findByCode("PRIY4821")).thenReturn(Optional.of(referralCode));
            when(referralCodeRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            referralService.checkAndIssueReferrerCredit(referee);

            // Verify referrer wallet credited
            assertEquals(new BigDecimal("600.00"), referrerWallet.getBalance());

            // Verify event updated
            assertEquals(ReferralStatus.CREDIT_ISSUED, referralEvent.getStatus());
            assertTrue(referralEvent.isReferrerCreditIssued());
            assertNotNull(referralEvent.getFirstBookingAt());

            // Verify code stats updated
            assertEquals(1, referralCode.getSuccessfulReferrals());
            assertEquals(new BigDecimal("100.00"), referralCode.getTotalCreditsEarned());

            // Verify notification sent to referrer
            verify(notificationService).sendToUser(eq(referrer.getId()),
                    eq(NotificationType.REFERRAL_BONUS_CREDIT), anyMap());
        }

        @Test
        @DisplayName("booking completion after expiry window does not issue credit")
        void expiredDoesNotIssueCredit() {
            referralEvent.setSignupAt(Instant.now().minus(40, ChronoUnit.DAYS)); // Past 30-day window
            when(referralEventRepository.findByRefereeId(referee.getId())).thenReturn(Optional.of(referralEvent));
            when(referralConfig.getSignupExpiryDays()).thenReturn(30);
            when(referralEventRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            referralService.checkAndIssueReferrerCredit(referee);

            assertEquals(ReferralStatus.EXPIRED, referralEvent.getStatus());
            verify(walletService, never()).getOrCreateWallet(referrer.getId());
            verify(notificationService, never()).sendToUser(eq(referrer.getId()), any(), anyMap());
        }

        @Test
        @DisplayName("second booking completion does not issue credit again")
        void secondBookingNoCredit() {
            referralEvent.setStatus(ReferralStatus.CREDIT_ISSUED);
            referralEvent.setReferrerCreditIssued(true);
            when(referralEventRepository.findByRefereeId(referee.getId())).thenReturn(Optional.of(referralEvent));

            referralService.checkAndIssueReferrerCredit(referee);

            verify(walletService, never()).getOrCreateWallet(any());
            verify(notificationService, never()).sendToUser(any(), any(), anyMap());
        }

        @Test
        @DisplayName("no referral event for customer → does nothing")
        void noReferralEvent() {
            when(referralEventRepository.findByRefereeId(referee.getId())).thenReturn(Optional.empty());

            referralService.checkAndIssueReferrerCredit(referee);

            verify(walletService, never()).getOrCreateWallet(any());
            verify(referralEventRepository, never()).save(any());
        }
    }

    // ─── getStats ─────────────────────────────────────────────────────

    @Nested
    @DisplayName("getStats")
    class GetStats {

        @Test
        @DisplayName("returns stats for user with referral code")
        void returnsStatsForExistingCode() {
            referralCode.setTotalReferrals(5);
            referralCode.setSuccessfulReferrals(3);
            referralCode.setTotalCreditsEarned(new BigDecimal("300.00"));
            when(referralCodeRepository.findByUserId(referrer.getId())).thenReturn(Optional.of(referralCode));

            ReferralStatsResponse stats = referralService.getStats(referrer.getId());

            assertEquals("PRIY4821", stats.getCode());
            assertEquals(5, stats.getTotalReferrals());
            assertEquals(3, stats.getSuccessfulReferrals());
            assertEquals(new BigDecimal("300.00"), stats.getTotalCreditsEarned());
        }

        @Test
        @DisplayName("returns zero stats when no referral code exists")
        void returnsZeroStatsWhenNoCode() {
            when(referralCodeRepository.findByUserId(referrer.getId())).thenReturn(Optional.empty());

            ReferralStatsResponse stats = referralService.getStats(referrer.getId());

            assertNull(stats.getCode());
            assertEquals(0, stats.getTotalReferrals());
            assertEquals(0, stats.getSuccessfulReferrals());
            assertEquals(BigDecimal.ZERO, stats.getTotalCreditsEarned());
        }
    }

    // ─── generateUniqueCode ───────────────────────────────────────────

    @Nested
    @DisplayName("generateUniqueCode")
    class GenerateUniqueCode {

        @Test
        @DisplayName("generates code with name prefix and correct length")
        void generatesCodeWithNamePrefix() {
            when(referralConfig.getCodeLength()).thenReturn(8);
            when(referralCodeRepository.existsByCode(anyString())).thenReturn(false);

            String code = referralService.generateUniqueCode("Priya");

            assertTrue(code.startsWith("PRIY"));
            assertEquals(8, code.length());
        }

        @Test
        @DisplayName("handles short names")
        void handlesShortNames() {
            when(referralConfig.getCodeLength()).thenReturn(8);
            when(referralCodeRepository.existsByCode(anyString())).thenReturn(false);

            String code = referralService.generateUniqueCode("Al");

            assertTrue(code.startsWith("AL"));
            assertNotNull(code);
        }

        @Test
        @DisplayName("handles null name with fallback prefix")
        void handlesNullName() {
            when(referralConfig.getCodeLength()).thenReturn(8);
            when(referralCodeRepository.existsByCode(anyString())).thenReturn(false);

            String code = referralService.generateUniqueCode(null);

            assertTrue(code.startsWith("HOME"));
        }
    }
}

