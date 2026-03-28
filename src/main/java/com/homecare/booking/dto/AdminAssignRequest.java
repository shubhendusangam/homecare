package com.homecare.booking.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class AdminAssignRequest {

    @NotNull(message = "Helper ID is required")
    private UUID helperId;
}

