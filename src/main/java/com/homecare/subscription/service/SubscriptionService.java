package com.homecare.subscription.service;

import com.homecare.core.dto.PagedResponse;
import com.homecare.core.enums.CyclePeriod;
import com.homecare.core.enums.ErrorCode;
import com.homecare.core.enums.SubscriptionStatus;
import com.homecare.core.exception.BusinessException;
import com.homecare.core.exception.ResourceNotFoundException;
import com.homecare.core.logging.AuditEvent;
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
import com.homecare.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.quartz.*;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class SubscriptionService {

    private final SubscriptionPlanRepository planRepository;
    private final CustomerSubscriptionRepository subscriptionRepository;
    private final UserRepository userRepository;
    private final WalletService walletService;
    private final NotificationService notificationService;
    private final ApplicationEventPublisher eventPublisher;
    private final Scheduler quartzScheduler;
    private final SubscriptionPlanResponseMapper planMapper;
    private final CustomerSubscriptionResponseMapper subscriptionMapper;

    // ─── Plan Management (Admin) ─────────────────────────────────────

    @Transactional
    public SubscriptionPlanResponse createPlan(CreateSubscriptionPlanRequest request) {
        SubscriptionPlan plan = SubscriptionPlan.builder()
                .name(request.getName())
                .description(request.getDescription())
                .serviceType(request.getServiceType())
                .cyclePeriod(request.getCyclePeriod())
                .sessionsPerCycle(request.getSessionsPerCycle())
                .durationHoursPerSession(request.getDurationHoursPerSession())
                .pricePerCycle(request.getPricePerCycle())
                .discountPercentage(request.getDiscountPercentage() != null
                        ? request.getDiscountPercentage() : BigDecimal.ZERO)
                .active(true)
                .build();
        plan = planRepository.save(plan);
        log.info("Subscription plan created: id={}, name={}", plan.getId(), plan.getName());
        return planMapper.toDto(plan);
    }

    @Transactional
    public SubscriptionPlanResponse updatePlan(UUID planId, UpdateSubscriptionPlanRequest request) {
        SubscriptionPlan plan = findPlan(planId);

        if (request.getName() != null) plan.setName(request.getName());
        if (request.getDescription() != null) plan.setDescription(request.getDescription());
        if (request.getServiceType() != null) plan.setServiceType(request.getServiceType());
        if (request.getCyclePeriod() != null) plan.setCyclePeriod(request.getCyclePeriod());
        if (request.getSessionsPerCycle() != null) plan.setSessionsPerCycle(request.getSessionsPerCycle());
        if (request.getDurationHoursPerSession() != null) plan.setDurationHoursPerSession(request.getDurationHoursPerSession());
        if (request.getPricePerCycle() != null) plan.setPricePerCycle(request.getPricePerCycle());
        if (request.getDiscountPercentage() != null) plan.setDiscountPercentage(request.getDiscountPercentage());

        plan = planRepository.save(plan);
        log.info("Subscription plan updated: id={}", planId);
        return planMapper.toDto(plan);
    }

    @Transactional
    public void deactivatePlan(UUID planId) {
        SubscriptionPlan plan = findPlan(planId);
        plan.setActive(false);
        planRepository.save(plan);
        log.info("Subscription plan deactivated: id={}", planId);
    }

    @Transactional(readOnly = true)
    public PagedResponse<SubscriptionPlanResponse> listActivePlans(Pageable pageable) {
        Page<SubscriptionPlanResponse> page = planRepository.findByActiveTrue(pageable)
                .map(planMapper::toDto);
        return PagedResponse.from(page);
    }

    @Transactional(readOnly = true)
    public PagedResponse<SubscriptionPlanResponse> listAllPlans(Pageable pageable) {
        Page<SubscriptionPlanResponse> page = planRepository.findAll(pageable)
                .map(planMapper::toDto);
        return PagedResponse.from(page);
    }

    @Transactional(readOnly = true)
    public SubscriptionPlanResponse getPlan(UUID planId) {
        return planMapper.toDto(findPlan(planId));
    }

    // ─── Customer Subscribe ──────────────────────────────────────────

    @Transactional
    public CustomerSubscriptionResponse subscribe(UUID customerId, SubscribeRequest request) {
        User customer = userRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", customerId));

        SubscriptionPlan plan = findPlan(request.getPlanId());
        if (!plan.isActive()) {
            throw new BusinessException("This subscription plan is no longer active", ErrorCode.PLAN_NOT_ACTIVE);
        }

        // Check for duplicate active subscription for the same plan
        if (subscriptionRepository.existsByCustomerIdAndPlanIdAndStatus(
                customerId, plan.getId(), SubscriptionStatus.ACTIVE)) {
            throw new BusinessException(
                    "You already have an active subscription for this plan",
                    ErrorCode.SUBSCRIPTION_ALREADY_ACTIVE);
        }

        // Debit wallet
        walletService.debitForSubscription(customerId, plan.getPricePerCycle(), UUID.randomUUID());

        // Compute cycle dates
        Instant now = Instant.now();
        Instant cycleEnd = computeCycleEnd(now, plan.getCyclePeriod());
        Instant nextRenewal = cycleEnd;

        CustomerSubscription subscription = CustomerSubscription.builder()
                .customer(customer)
                .plan(plan)
                .status(SubscriptionStatus.ACTIVE)
                .currentCycleStart(now)
                .currentCycleEnd(cycleEnd)
                .nextRenewalAt(nextRenewal)
                .sessionsUsedThisCycle(0)
                .build();
        subscription = subscriptionRepository.save(subscription);

        // Schedule renewal Quartz job
        scheduleRenewalJob(subscription);

        // Send notification
        notificationService.sendToUser(customerId, NotificationType.SUBSCRIPTION_STARTED,
                Map.of("planName", plan.getName(),
                       "serviceType", plan.getServiceType().name(),
                       "amount", plan.getPricePerCycle().toPlainString(),
                       "nextRenewalDate", nextRenewal.toString()));

        log.info("Customer {} subscribed to plan {}: subscription={}", customerId, plan.getId(), subscription.getId());
        eventPublisher.publishEvent(AuditEvent.of("SUBSCRIPTION_STARTED", customerId,
                Map.of("subscriptionId", subscription.getId(), "planId", plan.getId())));

        return subscriptionMapper.toDto(subscription);
    }

    // ─── Customer Cancel ─────────────────────────────────────────────

    @Transactional
    public CustomerSubscriptionResponse cancelSubscription(UUID customerId, UUID subscriptionId,
                                                            CancelSubscriptionRequest request) {
        CustomerSubscription subscription = findSubscription(subscriptionId);

        // Validate ownership
        if (!subscription.getCustomer().getId().equals(customerId)) {
            throw new BusinessException("You do not own this subscription", ErrorCode.FORBIDDEN);
        }

        if (subscription.getStatus() == SubscriptionStatus.CANCELLED) {
            throw new BusinessException("Subscription is already cancelled",
                    ErrorCode.SUBSCRIPTION_CANNOT_BE_CANCELLED);
        }

        subscription.setStatus(SubscriptionStatus.CANCELLED);
        subscription.setCancelledAt(Instant.now());
        subscription.setCancellationReason(request != null ? request.getReason() : null);
        subscriptionRepository.save(subscription);

        // Cancel Quartz renewal job
        cancelRenewalJob(subscriptionId);

        notificationService.sendToUser(customerId, NotificationType.SUBSCRIPTION_CANCELLED,
                Map.of("planName", subscription.getPlan().getName(),
                       "serviceType", subscription.getPlan().getServiceType().name()));

        log.info("Subscription {} cancelled by customer {}", subscriptionId, customerId);
        eventPublisher.publishEvent(AuditEvent.of("SUBSCRIPTION_CANCELLED", customerId,
                Map.of("subscriptionId", subscriptionId)));

        return subscriptionMapper.toDto(subscription);
    }

    // ─── Customer: My Subscriptions ──────────────────────────────────

    @Transactional(readOnly = true)
    public PagedResponse<CustomerSubscriptionResponse> getMySubscriptions(UUID customerId, Pageable pageable) {
        Page<CustomerSubscriptionResponse> page = subscriptionRepository
                .findByCustomerId(customerId, pageable)
                .map(subscriptionMapper::toDto);
        return PagedResponse.from(page);
    }

    @Transactional(readOnly = true)
    public CustomerSubscriptionResponse getSubscription(UUID subscriptionId) {
        return subscriptionMapper.toDto(findSubscription(subscriptionId));
    }

    // ─── Renewal (called by Quartz job) ──────────────────────────────

    @Transactional
    public void renewSubscription(UUID subscriptionId) {
        CustomerSubscription subscription = findSubscription(subscriptionId);

        if (subscription.getStatus() != SubscriptionStatus.ACTIVE) {
            log.info("Subscription {} is not ACTIVE (status={}), skipping renewal",
                    subscriptionId, subscription.getStatus());
            return;
        }

        UUID customerId = subscription.getCustomer().getId();
        SubscriptionPlan plan = subscription.getPlan();

        // Attempt wallet debit
        try {
            walletService.debitForSubscription(customerId, plan.getPricePerCycle(), subscriptionId);
        } catch (BusinessException e) {
            if (e.getErrorCode() == ErrorCode.INSUFFICIENT_WALLET_BALANCE) {
                // Pause subscription
                subscription.setStatus(SubscriptionStatus.PAUSED);
                subscriptionRepository.save(subscription);

                notificationService.sendToUser(customerId, NotificationType.WALLET_INSUFFICIENT_FOR_RENEWAL,
                        Map.of("planName", plan.getName(),
                               "amount", plan.getPricePerCycle().toPlainString(),
                               "balance", "insufficient"));

                log.warn("Subscription {} paused due to insufficient wallet balance", subscriptionId);
                return;
            }
            throw e;
        }

        // Advance cycle dates
        Instant newCycleStart = subscription.getCurrentCycleEnd();
        Instant newCycleEnd = computeCycleEnd(newCycleStart, plan.getCyclePeriod());

        subscription.setCurrentCycleStart(newCycleStart);
        subscription.setCurrentCycleEnd(newCycleEnd);
        subscription.setNextRenewalAt(newCycleEnd);
        subscription.setSessionsUsedThisCycle(0);
        subscriptionRepository.save(subscription);

        // Schedule next renewal
        scheduleRenewalJob(subscription);

        notificationService.sendToUser(customerId, NotificationType.SUBSCRIPTION_RENEWED,
                Map.of("planName", plan.getName(),
                       "serviceType", plan.getServiceType().name(),
                       "amount", plan.getPricePerCycle().toPlainString(),
                       "nextRenewalDate", newCycleEnd.toString()));

        log.info("Subscription {} renewed successfully. Next renewal at {}", subscriptionId, newCycleEnd);
        eventPublisher.publishEvent(AuditEvent.of("SUBSCRIPTION_RENEWED", customerId,
                Map.of("subscriptionId", subscriptionId)));
    }

    // ─── Increment Session Used ──────────────────────────────────────

    @Transactional
    public void incrementSessionUsed(UUID subscriptionId) {
        CustomerSubscription subscription = findSubscription(subscriptionId);

        if (subscription.getSessionsUsedThisCycle() >= subscription.getPlan().getSessionsPerCycle()) {
            throw new BusinessException(
                    "All sessions for this cycle have been used. Sessions used: "
                            + subscription.getSessionsUsedThisCycle()
                            + "/" + subscription.getPlan().getSessionsPerCycle(),
                    ErrorCode.SUBSCRIPTION_CANNOT_BE_CANCELLED);
        }

        subscription.setSessionsUsedThisCycle(subscription.getSessionsUsedThisCycle() + 1);
        subscriptionRepository.save(subscription);

        log.debug("Subscription {} session incremented: {}/{}",
                subscriptionId, subscription.getSessionsUsedThisCycle(),
                subscription.getPlan().getSessionsPerCycle());
    }

    // ─── Admin Methods ───────────────────────────────────────────────

    @Transactional(readOnly = true)
    public PagedResponse<CustomerSubscriptionResponse> getAllSubscriptions(SubscriptionStatus status,
                                                                           Pageable pageable) {
        Page<CustomerSubscriptionResponse> page;
        if (status != null) {
            page = subscriptionRepository.findByStatus(status, pageable)
                    .map(subscriptionMapper::toDto);
        } else {
            page = subscriptionRepository.findAll(pageable)
                    .map(subscriptionMapper::toDto);
        }
        return PagedResponse.from(page);
    }

    @Transactional
    public CustomerSubscriptionResponse adminCancelSubscription(UUID subscriptionId, String reason) {
        CustomerSubscription subscription = findSubscription(subscriptionId);

        if (subscription.getStatus() == SubscriptionStatus.CANCELLED) {
            throw new BusinessException("Subscription is already cancelled",
                    ErrorCode.SUBSCRIPTION_CANNOT_BE_CANCELLED);
        }

        subscription.setStatus(SubscriptionStatus.CANCELLED);
        subscription.setCancelledAt(Instant.now());
        subscription.setCancellationReason(reason != null ? reason : "Cancelled by admin");
        subscriptionRepository.save(subscription);

        cancelRenewalJob(subscriptionId);

        UUID customerId = subscription.getCustomer().getId();
        notificationService.sendToUser(customerId, NotificationType.SUBSCRIPTION_CANCELLED,
                Map.of("planName", subscription.getPlan().getName(),
                       "serviceType", subscription.getPlan().getServiceType().name()));

        log.info("Subscription {} cancelled by admin. Reason: {}", subscriptionId, reason);
        eventPublisher.publishEvent(AuditEvent.of("SUBSCRIPTION_ADMIN_CANCELLED", customerId,
                Map.of("subscriptionId", subscriptionId, "reason", reason != null ? reason : "")));

        return subscriptionMapper.toDto(subscription);
    }

    // ─── Private Helpers ─────────────────────────────────────────────

    private SubscriptionPlan findPlan(UUID planId) {
        return planRepository.findById(planId)
                .orElseThrow(() -> new ResourceNotFoundException("SubscriptionPlan", "id", planId));
    }

    private CustomerSubscription findSubscription(UUID subscriptionId) {
        return subscriptionRepository.findById(subscriptionId)
                .orElseThrow(() -> new ResourceNotFoundException("CustomerSubscription", "id", subscriptionId));
    }

    private Instant computeCycleEnd(Instant start, CyclePeriod period) {
        return switch (period) {
            case WEEKLY -> start.plus(Duration.ofDays(7));
            case BIWEEKLY -> start.plus(Duration.ofDays(14));
            case MONTHLY -> start.plus(Duration.ofDays(30));
        };
    }

    private void scheduleRenewalJob(CustomerSubscription subscription) {
        try {
            JobKey jobKey = JobKey.jobKey("renew-subscription-" + subscription.getId(), "subscription-renewal");
            // Remove existing job if any
            quartzScheduler.deleteJob(jobKey);

            JobDetail jobDetail = JobBuilder.newJob()
                    .ofType(getSubscriptionRenewalJobClass())
                    .withIdentity(jobKey)
                    .usingJobData("subscriptionId", subscription.getId().toString())
                    .build();

            Trigger trigger = TriggerBuilder.newTrigger()
                    .withIdentity("renew-trigger-" + subscription.getId(), "subscription-renewal")
                    .startAt(Date.from(subscription.getNextRenewalAt()))
                    .build();

            quartzScheduler.scheduleJob(jobDetail, trigger);
            log.debug("Scheduled renewal job for subscription {} at {}",
                    subscription.getId(), subscription.getNextRenewalAt());
        } catch (SchedulerException e) {
            log.error("Failed to schedule renewal job for subscription {}", subscription.getId(), e);
        }
    }

    private void cancelRenewalJob(UUID subscriptionId) {
        try {
            boolean deleted = quartzScheduler.deleteJob(
                    JobKey.jobKey("renew-subscription-" + subscriptionId, "subscription-renewal"));
            if (deleted) {
                log.debug("Renewal job cancelled for subscription {}", subscriptionId);
            }
        } catch (SchedulerException e) {
            log.error("Failed to cancel renewal job for subscription {}", subscriptionId, e);
        }
    }

    @SuppressWarnings("unchecked")
    private Class<? extends Job> getSubscriptionRenewalJobClass() {
        try {
            return (Class<? extends Job>) Class.forName("com.homecare.scheduler.SubscriptionRenewalJob");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("SubscriptionRenewalJob class not found", e);
        }
    }
}

