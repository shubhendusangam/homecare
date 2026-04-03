package com.homecare.subscription.repository;

import com.homecare.core.enums.SubscriptionStatus;
import com.homecare.subscription.entity.CustomerSubscription;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface CustomerSubscriptionRepository extends JpaRepository<CustomerSubscription, UUID> {

    List<CustomerSubscription> findByCustomerIdAndStatus(UUID customerId, SubscriptionStatus status);

    Page<CustomerSubscription> findByCustomerId(UUID customerId, Pageable pageable);

    List<CustomerSubscription> findByStatusAndNextRenewalAtBefore(SubscriptionStatus status, Instant before);

    boolean existsByCustomerIdAndPlanIdAndStatus(UUID customerId, UUID planId, SubscriptionStatus status);

    Page<CustomerSubscription> findByStatus(SubscriptionStatus status, Pageable pageable);
}

