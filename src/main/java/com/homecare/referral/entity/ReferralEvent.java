package com.homecare.referral.entity;

import com.homecare.core.entity.BaseEntity;
import com.homecare.core.enums.ReferralStatus;
import com.homecare.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "referral_events")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReferralEvent extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "referrer_id", nullable = false)
    private User referrer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "referee_id", nullable = false)
    private User referee;

    @Column(nullable = false)
    private String referralCode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReferralStatus status;

    private Instant signupAt;

    private Instant firstBookingAt;

    @Builder.Default
    @Column(precision = 10, scale = 2)
    private BigDecimal referrerCredit = BigDecimal.ZERO;

    @Builder.Default
    @Column(precision = 10, scale = 2)
    private BigDecimal refereeCredit = BigDecimal.ZERO;

    @Builder.Default
    private boolean referrerCreditIssued = false;

    @Builder.Default
    private boolean refereeCreditIssued = false;
}

