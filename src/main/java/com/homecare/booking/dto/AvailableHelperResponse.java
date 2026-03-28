package com.homecare.booking.dto;

import com.homecare.core.enums.ServiceType;
import com.homecare.user.enums.HelperStatus;
import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
@Builder
public class AvailableHelperResponse {

    private UUID helperId;
    private UUID userId;
    private String name;
    private String avatarUrl;
    private List<ServiceType> skills;
    private double latitude;
    private double longitude;
    private double distanceKm;
    private double rating;
    private int totalJobsCompleted;
    private HelperStatus status;
}

