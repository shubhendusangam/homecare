package com.homecare.subscription.repository;

import com.homecare.core.enums.ServiceType;
import com.homecare.subscription.entity.SubscriptionPlan;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface SubscriptionPlanRepository extends JpaRepository<SubscriptionPlan, UUID> {

    List<SubscriptionPlan> findByActiveTrue();

    Page<SubscriptionPlan> findByActiveTrue(Pageable pageable);

    List<SubscriptionPlan> findByServiceTypeAndActiveTrue(ServiceType serviceType);
}

