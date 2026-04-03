package com.homecare.referral.entity;

import com.homecare.core.entity.BaseEntity;
import com.homecare.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "referral_codes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReferralCode extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(unique = true, nullable = false, length = 20)
    private String code;

    @Builder.Default
    private int totalReferrals = 0;

    @Builder.Default
    private int successfulReferrals = 0;

    @Builder.Default
    @Column(precision = 10, scale = 2)
    private BigDecimal totalCreditsEarned = BigDecimal.ZERO;
}

