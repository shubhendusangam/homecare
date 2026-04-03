package com.homecare.referral.dto;

import com.homecare.core.enums.ReferralStatus;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Getter
@Builder
public class ReferralEventResponse {
    private UUID id;
    private UUID referrerId;
    private String referrerName;
    private UUID refereeId;
    private String refereeName;
    private String referralCode;
    private ReferralStatus status;
    private Instant signupAt;
    private Instant firstBookingAt;
    private BigDecimal referrerCredit;
    private BigDecimal refereeCredit;
    private boolean referrerCreditIssued;
    private boolean refereeCreditIssued;
    private Instant createdAt;
}

