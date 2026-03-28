package com.homecare.admin.dto;

import com.homecare.core.enums.ServiceType;
import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class ServiceConfigDto {
    private UUID id;
    private ServiceType serviceType;
    private String name;
    private double basePrice;
    private double perHourPrice;
    private String icon;
    private boolean active;
}

