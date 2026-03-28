package com.homecare.payment.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Builder
public class WalletResponse {
    private UUID walletId;
    private UUID userId;
    private BigDecimal balance;
    private BigDecimal heldAmount;
    private BigDecimal availableBalance;
    private String currency;
}

