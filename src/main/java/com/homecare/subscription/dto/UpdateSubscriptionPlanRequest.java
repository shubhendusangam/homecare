package com.homecare.subscription.dto;

import com.homecare.core.enums.CyclePeriod;
import com.homecare.core.enums.ServiceType;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class UpdateSubscriptionPlanRequest {

    private String name;

    private String description;

    private ServiceType serviceType;

    private CyclePeriod cyclePeriod;

    @Min(value = 1, message = "Sessions per cycle must be at least 1")
    private Integer sessionsPerCycle;

    @Min(value = 1, message = "Duration per session must be at least 1 hour")
    @Max(value = 8, message = "Duration per session must be at most 8 hours")
    private Integer durationHoursPerSession;

    @DecimalMin(value = "0.01", message = "Price must be greater than 0")
    private BigDecimal pricePerCycle;

    @DecimalMin(value = "0.00", message = "Discount percentage must be >= 0")
    @DecimalMax(value = "100.00", message = "Discount percentage must be <= 100")
    private BigDecimal discountPercentage;
}

