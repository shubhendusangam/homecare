package com.homecare.subscription.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class SubscribeRequest {

    @NotNull(message = "Plan ID is required")
    private UUID planId;
}

