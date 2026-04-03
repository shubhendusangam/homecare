package com.homecare.scheduler;

import com.homecare.core.enums.SubscriptionStatus;
import com.homecare.subscription.entity.CustomerSubscription;
import com.homecare.subscription.repository.CustomerSubscriptionRepository;
import com.homecare.subscription.service.SubscriptionService;
import lombok.extern.slf4j.Slf4j;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

/**
 * Daily sweep job (cron: 1:00 AM) that catches any missed per-subscription
 * one-shot renewal triggers. Queries for ACTIVE subscriptions whose
 * {@code nextRenewalAt} has passed and delegates each to
 * {@link SubscriptionService#renewSubscription}.
 */
@Component
@Slf4j
public class SubscriptionRenewalSweepJob implements Job {

    @Autowired
    private CustomerSubscriptionRepository subscriptionRepository;

    @Autowired
    private SubscriptionService subscriptionService;

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        log.info("SubscriptionRenewalSweepJob started — checking for missed renewals");

        List<CustomerSubscription> dueSubscriptions = subscriptionRepository
                .findByStatusAndNextRenewalAtBefore(SubscriptionStatus.ACTIVE, Instant.now());

        log.info("Found {} subscriptions due for renewal", dueSubscriptions.size());

        int success = 0;
        int failed = 0;
        for (CustomerSubscription sub : dueSubscriptions) {
            try {
                subscriptionService.renewSubscription(sub.getId());
                success++;
            } catch (Exception e) {
                failed++;
                log.error("Failed to renew subscription {} during sweep: {}",
                        sub.getId(), e.getMessage());
            }
        }

        log.info("SubscriptionRenewalSweepJob completed — renewed: {}, failed: {}", success, failed);
    }
}

