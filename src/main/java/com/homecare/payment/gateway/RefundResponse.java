package com.homecare.payment.gateway;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class RefundResponse {
    private String refundId;
    private String status;
    private BigDecimal amount;
}

