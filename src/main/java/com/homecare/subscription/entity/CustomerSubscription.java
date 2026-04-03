package com.homecare.subscription.entity;

import com.homecare.core.entity.BaseEntity;
import com.homecare.core.enums.SubscriptionStatus;
import com.homecare.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "customer_subscriptions", indexes = {
        @Index(name = "idx_sub_customer_status", columnList = "customer_id, status"),
        @Index(name = "idx_sub_renewal_status", columnList = "next_renewal_at, status")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomerSubscription extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private User customer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plan_id", nullable = false)
    private SubscriptionPlan plan;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private SubscriptionStatus status = SubscriptionStatus.ACTIVE;

    @Column(nullable = false)
    private Instant currentCycleStart;

    @Column(nullable = false)
    private Instant currentCycleEnd;

    private Instant nextRenewalAt;

    @Builder.Default
    private int sessionsUsedThisCycle = 0;

    private Instant cancelledAt;

    @Column(length = 1000)
    private String cancellationReason;
}

