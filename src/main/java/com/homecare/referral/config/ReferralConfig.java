package com.homecare.referral.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;

@Configuration
@ConfigurationProperties(prefix = "homecare.referral")
@Getter
@Setter
public class ReferralConfig {

    /**
     * Amount credited to the referrer after referee's first completed booking.
     */
    private BigDecimal referrerCredit = new BigDecimal("100.00");

    /**
     * Amount credited to the referee immediately on registration with a referral code.
     */
    private BigDecimal refereeCredit = new BigDecimal("50.00");

    /**
     * Length of auto-generated referral codes.
     */
    private int codeLength = 8;

    /**
     * Number of days after signup within which the referee must complete their first booking
     * for the referrer to receive credit.
     */
    private int signupExpiryDays = 30;
}

