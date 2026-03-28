package com.homecare.payment.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Builder
public class EarningsSummaryResponse {
    private BigDecimal totalEarnings;
    private BigDecimal availableBalance;
    private String currency;
    private long totalCompletedJobs;
    private List<WalletTransactionResponse> recentTransactions;
}

