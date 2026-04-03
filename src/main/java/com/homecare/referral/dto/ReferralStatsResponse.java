package com.homecare.referral.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class ReferralStatsResponse {
    private String code;
    private int totalReferrals;
    private int successfulReferrals;
    private BigDecimal totalCreditsEarned;
}

