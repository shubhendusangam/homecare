package com.homecare.admin.dto;

import jakarta.validation.constraints.Min;
import lombok.Data;

@Data
public class UpdateServiceConfigRequest {

    @Min(value = 0, message = "Base price must be non-negative")
    private Double basePrice;

    @Min(value = 0, message = "Per-hour price must be non-negative")
    private Double perHourPrice;

    private Boolean active;
}

