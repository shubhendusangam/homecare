package com.homecare.dispute.dto;

import com.homecare.core.enums.EvidenceType;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
public class EvidenceResponse {

    private UUID id;
    private UUID submittedById;
    private String submittedByName;
    private EvidenceType type;
    private String content;
    private String description;
    private Instant createdAt;
}

