package com.homecare.subscription.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CancelSubscriptionRequest {

    @Size(max = 1000, message = "Reason must be under 1000 characters")
    private String reason;
}

