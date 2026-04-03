package com.homecare.payment.entity;

import com.homecare.booking.entity.Booking;
import com.homecare.core.entity.BaseEntity;
import com.homecare.payment.enums.TransactionStatus;
import com.homecare.payment.enums.TransactionType;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "wallet_transactions", indexes = {
        @Index(name = "idx_wallet_tx_wallet_type", columnList = "wallet_id, type"),
        @Index(name = "idx_wallet_tx_booking", columnList = "booking_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WalletTransaction extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "wallet_id", nullable = false)
    private Wallet wallet;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id")
    private Booking booking;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransactionType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private TransactionStatus status = TransactionStatus.PENDING;

    @Column(precision = 10, scale = 2, nullable = false)
    private BigDecimal amount;

    private String description;

    private String externalReference;

    private Instant processedAt;
}

