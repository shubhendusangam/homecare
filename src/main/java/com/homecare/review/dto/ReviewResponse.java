package com.homecare.review.dto;

import com.homecare.core.enums.ServiceType;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Builder
public class ReviewResponse {
    private UUID id;
    private UUID bookingId;
    private UUID customerId;
    private String customerName;
    private UUID helperId;
    private String helperName;
    private int rating;
    private String comment;
    private boolean published;
    private boolean flagged;
    private ServiceType serviceType;
    private Instant createdAt;
}

