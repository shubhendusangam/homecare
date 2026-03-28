package com.homecare.payment.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class PaymentSummaryResponse {
    private BigDecimal totalRevenue;
    private long totalTransactions;
    private BigDecimal totalRefunds;
    private String period; // "daily" or "weekly"
}

