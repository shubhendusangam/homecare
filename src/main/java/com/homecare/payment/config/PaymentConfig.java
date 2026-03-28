package com.homecare.payment.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;

@Configuration
@ConfigurationProperties(prefix = "homecare.payment")
@Getter
@Setter
public class PaymentConfig {

    /**
     * Alert threshold for low wallet balance.
     */
    private BigDecimal walletLowBalanceAlert = new BigDecimal("100");

    /**
     * Platform fee percentage (0.15 = 15%).
     * Helper receives (1 - platformFee) of booking amount.
     */
    private BigDecimal platformFee = new BigDecimal("0.15");

    /**
     * Razorpay key_id — sent to frontend for checkout.
     */
    private String razorpayKeyId = "rzp_test_mock_key";

    /**
     * Default currency.
     */
    private String defaultCurrency = "INR";
}

