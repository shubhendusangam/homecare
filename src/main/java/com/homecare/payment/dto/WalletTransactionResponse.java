package com.homecare.payment.dto;

import com.homecare.payment.enums.TransactionStatus;
import com.homecare.payment.enums.TransactionType;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Getter
@Builder
public class WalletTransactionResponse {
    private UUID id;
    private UUID walletId;
    private UUID bookingId;
    private TransactionType type;
    private TransactionStatus status;
    private BigDecimal amount;
    private String description;
    private String externalReference;
    private Instant processedAt;
    private Instant createdAt;
}

