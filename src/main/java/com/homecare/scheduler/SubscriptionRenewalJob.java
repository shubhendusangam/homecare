package com.homecare.scheduler;

import com.homecare.subscription.service.SubscriptionService;
import lombok.extern.slf4j.Slf4j;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * One-shot Quartz job that renews a single subscription.
 * Scheduled by {@link SubscriptionService} when a subscription is created or renewed.
 * Reads {@code subscriptionId} from the job data map and delegates to
 * {@link SubscriptionService#renewSubscription(UUID)}.
 */
@Component
@Slf4j
public class SubscriptionRenewalJob implements Job {

    @Autowired
    private SubscriptionService subscriptionService;

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        String subscriptionIdStr = context.getJobDetail().getJobDataMap().getString("subscriptionId");
        if (subscriptionIdStr == null) {
            log.warn("SubscriptionRenewalJob fired without subscriptionId");
            return;
        }

        UUID subscriptionId = UUID.fromString(subscriptionIdStr);
        log.info("SubscriptionRenewalJob fired for subscription {}", subscriptionId);

        try {
            subscriptionService.renewSubscription(subscriptionId);
        } catch (Exception e) {
            log.error("Failed to renew subscription {}: {}", subscriptionId, e.getMessage(), e);
        }
    }
}

