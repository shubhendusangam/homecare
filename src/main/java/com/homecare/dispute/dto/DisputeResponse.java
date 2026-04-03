package com.homecare.dispute.dto;

import com.homecare.core.enums.*;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Data
@Builder
public class DisputeResponse {

    private UUID id;
    private UUID bookingId;

    private UUID raisedById;
    private String raisedByName;
    private DisputeRaisedBy raisedByRole;

    private DisputeType type;
    private DisputeStatus status;
    private String description;

    private DisputeResolution resolution;
    private String adminNotes;
    private UUID assignedAdminId;
    private String assignedAdminName;
    private Instant resolvedAt;
    private BigDecimal refundAmount;

    private List<EvidenceResponse> evidence;

    private Instant createdAt;
    private Instant updatedAt;
}

