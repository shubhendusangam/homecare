package com.homecare.referral.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ReferralCodeResponse {
    private String code;
    private int totalReferrals;
    private int successfulReferrals;
}

