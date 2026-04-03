package com.homecare.dispute.dto;

import com.homecare.core.enums.DisputeResolution;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class ResolveDisputeRequest {

    @NotNull(message = "Resolution is required")
    private DisputeResolution resolution;

    /** Required when resolution is PARTIAL_REFUND. */
    private BigDecimal refundAmount;

    @Size(max = 2000, message = "Admin notes must be under 2000 characters")
    private String adminNotes;
}

