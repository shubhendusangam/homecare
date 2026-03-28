package com.homecare.payment.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class TopupInitiateResponse {
    private String razorpayOrderId;
    private BigDecimal amount;
    private String currency;
    private String key; // Razorpay key_id for frontend
}

