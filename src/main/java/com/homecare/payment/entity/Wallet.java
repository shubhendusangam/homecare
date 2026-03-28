package com.homecare.payment.entity;

import com.homecare.core.entity.BaseEntity;
import com.homecare.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "wallets")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Wallet extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(precision = 10, scale = 2, nullable = false)
    @Builder.Default
    private BigDecimal balance = BigDecimal.ZERO;

    @Column(precision = 10, scale = 2, nullable = false)
    @Builder.Default
    private BigDecimal heldAmount = BigDecimal.ZERO;

    @Column(nullable = false)
    @Builder.Default
    private String currency = "INR";

    /**
     * Returns available balance (balance minus held amount).
     */
    public BigDecimal getAvailableBalance() {
        return balance.subtract(heldAmount);
    }
}

