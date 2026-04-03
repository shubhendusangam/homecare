package com.homecare.referral.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class AdminReferralSummaryResponse {
    private long totalSignups;
    private long totalConversions;
    private long totalExpired;
    private BigDecimal totalCreditsIssued;
    private double conversionRate;
}

