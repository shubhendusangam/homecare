package com.homecare.subscription.service;

import com.homecare.core.enums.CyclePeriod;
import com.homecare.core.enums.ErrorCode;
import com.homecare.core.enums.ServiceType;
import com.homecare.core.enums.SubscriptionStatus;
import com.homecare.core.exception.BusinessException;
import com.homecare.notification.enums.NotificationType;
import com.homecare.notification.service.NotificationService;
import com.homecare.payment.service.WalletService;
import com.homecare.subscription.dto.*;
import com.homecare.subscription.entity.CustomerSubscription;
import com.homecare.subscription.entity.SubscriptionPlan;
import com.homecare.subscription.mapper.CustomerSubscriptionResponseMapper;
import com.homecare.subscription.mapper.SubscriptionPlanResponseMapper;
import com.homecare.subscription.repository.CustomerSubscriptionRepository;
import com.homecare.subscription.repository.SubscriptionPlanRepository;
import com.homecare.user.entity.User;
import com.homecare.user.enums.Role;
import com.homecare.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.quartz.Scheduler;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SubscriptionService")
class SubscriptionServiceTest {

    @Mock private SubscriptionPlanRepository planRepository;
    @Mock private CustomerSubscriptionRepository subscriptionRepository;
    @Mock private UserRepository userRepository;
    @Mock private WalletService walletService;
    @Mock private NotificationService notificationService;
    @Mock private ApplicationEventPublisher eventPublisher;
    @Mock private Scheduler quartzScheduler;
    @Mock private SubscriptionPlanResponseMapper planMapper;
    @Mock private CustomerSubscriptionResponseMapper subscriptionMapper;

    @InjectMocks private SubscriptionService subscriptionService;

    private User customer;
    private SubscriptionPlan plan;
    private CustomerSubscription subscription;

    @BeforeEach
    void setUp() {
        customer = User.builder().name("Customer").email("c@test.com").role(Role.CUSTOMER).build();
        customer.setId(UUID.randomUUID());

        plan = SubscriptionPlan.builder()
                .name("Weekly Cleaning")
                .description("Weekly home cleaning service")
                .serviceType(ServiceType.CLEANING)
                .cyclePeriod(CyclePeriod.WEEKLY)
                .sessionsPerCycle(1)
                .durationHoursPerSession(2)
                .pricePerCycle(new BigDecimal("500.00"))
                .discountPercentage(BigDecimal.ZERO)
                .active(true)
                .build();
        plan.setId(UUID.randomUUID());

        subscription = CustomerSubscription.builder()
                .customer(customer)
                .plan(plan)
                .status(SubscriptionStatus.ACTIVE)
                .currentCycleStart(Instant.now())
                .currentCycleEnd(Instant.now().plusSeconds(7 * 24 * 3600))
                .nextRenewalAt(Instant.now().plusSeconds(7 * 24 * 3600))
                .sessionsUsedThisCycle(0)
                .build();
        subscription.setId(UUID.randomUUID());
    }

    // ─── Subscribe ───────────────────────────────────────────────────

    @Nested
    @DisplayName("Subscribe")
    class Subscribe {

        @Test
        @DisplayName("should subscribe successfully to an active plan")
        void subscribeHappyPath() {
            SubscribeRequest request = new SubscribeRequest();
            request.setPlanId(plan.getId());

            when(userRepository.findById(customer.getId())).thenReturn(Optional.of(customer));
            when(planRepository.findById(plan.getId())).thenReturn(Optional.of(plan));
            when(subscriptionRepository.existsByCustomerIdAndPlanIdAndStatus(
                    customer.getId(), plan.getId(), SubscriptionStatus.ACTIVE)).thenReturn(false);
            when(subscriptionRepository.save(any(CustomerSubscription.class)))
                    .thenAnswer(inv -> {
                        CustomerSubscription s = inv.getArgument(0);
                        s.setId(UUID.randomUUID());
                        return s;
                    });

            CustomerSubscriptionResponse expectedResponse = CustomerSubscriptionResponse.builder()
                    .id(UUID.randomUUID()).planName("Weekly Cleaning").build();
            when(subscriptionMapper.toDto(any(CustomerSubscription.class))).thenReturn(expectedResponse);

            CustomerSubscriptionResponse result = subscriptionService.subscribe(customer.getId(), request);

            assertNotNull(result);
            verify(walletService).debitForSubscription(eq(customer.getId()), eq(plan.getPricePerCycle()), any(UUID.class));
            verify(subscriptionRepository).save(any(CustomerSubscription.class));
            verify(notificationService).sendToUser(eq(customer.getId()),
                    eq(NotificationType.SUBSCRIPTION_STARTED), anyMap());
            verify(eventPublisher).publishEvent(any());
        }

        @Test
        @DisplayName("should throw when plan is not active")
        void subscribeInactivePlan() {
            plan.setActive(false);
            SubscribeRequest request = new SubscribeRequest();
            request.setPlanId(plan.getId());

            when(userRepository.findById(customer.getId())).thenReturn(Optional.of(customer));
            when(planRepository.findById(plan.getId())).thenReturn(Optional.of(plan));

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> subscriptionService.subscribe(customer.getId(), request));
            assertEquals(ErrorCode.PLAN_NOT_ACTIVE, ex.getErrorCode());
        }

        @Test
        @DisplayName("should throw when duplicate active subscription exists")
        void subscribeDuplicate() {
            SubscribeRequest request = new SubscribeRequest();
            request.setPlanId(plan.getId());

            when(userRepository.findById(customer.getId())).thenReturn(Optional.of(customer));
            when(planRepository.findById(plan.getId())).thenReturn(Optional.of(plan));
            when(subscriptionRepository.existsByCustomerIdAndPlanIdAndStatus(
                    customer.getId(), plan.getId(), SubscriptionStatus.ACTIVE)).thenReturn(true);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> subscriptionService.subscribe(customer.getId(), request));
            assertEquals(ErrorCode.SUBSCRIPTION_ALREADY_ACTIVE, ex.getErrorCode());
        }

        @Test
        @DisplayName("should throw when wallet has insufficient balance")
        void subscribeInsufficientWallet() {
            SubscribeRequest request = new SubscribeRequest();
            request.setPlanId(plan.getId());

            when(userRepository.findById(customer.getId())).thenReturn(Optional.of(customer));
            when(planRepository.findById(plan.getId())).thenReturn(Optional.of(plan));
            when(subscriptionRepository.existsByCustomerIdAndPlanIdAndStatus(
                    customer.getId(), plan.getId(), SubscriptionStatus.ACTIVE)).thenReturn(false);
            doThrow(new BusinessException("Insufficient wallet balance", ErrorCode.INSUFFICIENT_WALLET_BALANCE))
                    .when(walletService).debitForSubscription(eq(customer.getId()), any(), any());

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> subscriptionService.subscribe(customer.getId(), request));
            assertEquals(ErrorCode.INSUFFICIENT_WALLET_BALANCE, ex.getErrorCode());
        }
    }

    // ─── Cancel Subscription ─────────────────────────────────────────

    @Nested
    @DisplayName("CancelSubscription")
    class CancelSubscription {

        @Test
        @DisplayName("should cancel subscription successfully")
        void cancelHappyPath() {
            CancelSubscriptionRequest request = new CancelSubscriptionRequest();
            request.setReason("No longer needed");

            when(subscriptionRepository.findById(subscription.getId()))
                    .thenReturn(Optional.of(subscription));
            when(subscriptionRepository.save(any(CustomerSubscription.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            CustomerSubscriptionResponse expectedResponse = CustomerSubscriptionResponse.builder()
                    .id(subscription.getId()).status(SubscriptionStatus.CANCELLED).build();
            when(subscriptionMapper.toDto(any(CustomerSubscription.class))).thenReturn(expectedResponse);

            CustomerSubscriptionResponse result = subscriptionService
                    .cancelSubscription(customer.getId(), subscription.getId(), request);

            assertNotNull(result);
            assertEquals(SubscriptionStatus.CANCELLED, result.getStatus());
            verify(notificationService).sendToUser(eq(customer.getId()),
                    eq(NotificationType.SUBSCRIPTION_CANCELLED), anyMap());
        }

        @Test
        @DisplayName("should throw when user does not own the subscription")
        void cancelNotOwner() {
            UUID otherUserId = UUID.randomUUID();
            when(subscriptionRepository.findById(subscription.getId()))
                    .thenReturn(Optional.of(subscription));

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> subscriptionService.cancelSubscription(otherUserId, subscription.getId(), null));
            assertEquals(ErrorCode.FORBIDDEN, ex.getErrorCode());
        }

        @Test
        @DisplayName("should throw when subscription is already cancelled")
        void cancelAlreadyCancelled() {
            subscription.setStatus(SubscriptionStatus.CANCELLED);
            when(subscriptionRepository.findById(subscription.getId()))
                    .thenReturn(Optional.of(subscription));

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> subscriptionService.cancelSubscription(
                            customer.getId(), subscription.getId(), null));
            assertEquals(ErrorCode.SUBSCRIPTION_CANNOT_BE_CANCELLED, ex.getErrorCode());
        }
    }

    // ─── Renew Subscription ──────────────────────────────────────────

    @Nested
    @DisplayName("RenewSubscription")
    class RenewSubscription {

        @Test
        @DisplayName("should renew subscription successfully")
        void renewHappyPath() {
            when(subscriptionRepository.findById(subscription.getId()))
                    .thenReturn(Optional.of(subscription));
            when(subscriptionRepository.save(any(CustomerSubscription.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            subscriptionService.renewSubscription(subscription.getId());

            verify(walletService).debitForSubscription(
                    eq(customer.getId()), eq(plan.getPricePerCycle()), eq(subscription.getId()));
            verify(subscriptionRepository).save(argThat(s ->
                    s.getSessionsUsedThisCycle() == 0));
            verify(notificationService).sendToUser(eq(customer.getId()),
                    eq(NotificationType.SUBSCRIPTION_RENEWED), anyMap());
        }

        @Test
        @DisplayName("should pause subscription when wallet balance is insufficient")
        void renewInsufficientBalance() {
            when(subscriptionRepository.findById(subscription.getId()))
                    .thenReturn(Optional.of(subscription));
            doThrow(new BusinessException("Insufficient", ErrorCode.INSUFFICIENT_WALLET_BALANCE))
                    .when(walletService).debitForSubscription(
                            eq(customer.getId()), any(), eq(subscription.getId()));
            when(subscriptionRepository.save(any(CustomerSubscription.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            subscriptionService.renewSubscription(subscription.getId());

            verify(subscriptionRepository).save(argThat(s ->
                    s.getStatus() == SubscriptionStatus.PAUSED));
            verify(notificationService).sendToUser(eq(customer.getId()),
                    eq(NotificationType.WALLET_INSUFFICIENT_FOR_RENEWAL), anyMap());
        }

        @Test
        @DisplayName("should skip renewal when subscription is not ACTIVE")
        void renewSkipsNonActive() {
            subscription.setStatus(SubscriptionStatus.CANCELLED);
            when(subscriptionRepository.findById(subscription.getId()))
                    .thenReturn(Optional.of(subscription));

            subscriptionService.renewSubscription(subscription.getId());

            verify(walletService, never()).debitForSubscription(any(), any(), any());
            verify(subscriptionRepository, never()).save(any());
        }
    }

    // ─── Increment Session Used ──────────────────────────────────────

    @Nested
    @DisplayName("IncrementSessionUsed")
    class IncrementSessionUsed {

        @Test
        @DisplayName("should increment session count successfully")
        void incrementHappyPath() {
            subscription.setSessionsUsedThisCycle(0);
            when(subscriptionRepository.findById(subscription.getId()))
                    .thenReturn(Optional.of(subscription));
            when(subscriptionRepository.save(any(CustomerSubscription.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            subscriptionService.incrementSessionUsed(subscription.getId());

            verify(subscriptionRepository).save(argThat(s ->
                    s.getSessionsUsedThisCycle() == 1));
        }

        @Test
        @DisplayName("should throw when max sessions per cycle reached")
        void incrementMaxReached() {
            subscription.setSessionsUsedThisCycle(plan.getSessionsPerCycle());
            when(subscriptionRepository.findById(subscription.getId()))
                    .thenReturn(Optional.of(subscription));

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> subscriptionService.incrementSessionUsed(subscription.getId()));
            assertNotNull(ex.getErrorCode());
        }
    }
}


