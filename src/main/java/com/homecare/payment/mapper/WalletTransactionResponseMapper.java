package com.homecare.payment.mapper;

import com.homecare.payment.dto.WalletTransactionResponse;
import com.homecare.payment.entity.WalletTransaction;
import org.springframework.stereotype.Component;

/**
 * Centralised mapper for {@link WalletTransaction} → {@link WalletTransactionResponse}.
 * Eliminates the duplicate {@code toTransactionResponse} methods
 * that existed in PaymentService and WalletService.
 */
@Component
public class WalletTransactionResponseMapper {

    public WalletTransactionResponse toDto(WalletTransaction tx) {
        return WalletTransactionResponse.builder()
                .id(tx.getId())
                .walletId(tx.getWallet().getId())
                .bookingId(tx.getBooking() != null ? tx.getBooking().getId() : null)
                .type(tx.getType())
                .status(tx.getStatus())
                .amount(tx.getAmount())
                .description(tx.getDescription())
                .externalReference(tx.getExternalReference())
                .processedAt(tx.getProcessedAt())
                .createdAt(tx.getCreatedAt())
                .build();
    }
}

