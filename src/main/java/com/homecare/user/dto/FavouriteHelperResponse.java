package com.homecare.user.dto;

import com.homecare.core.enums.ServiceType;
import com.homecare.user.enums.HelperStatus;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Data
@Builder
public class FavouriteHelperResponse {

    private UUID helperId;
    private String helperName;
    private String avatarUrl;
    private List<ServiceType> skills;
    private double rating;
    private int totalJobsCompleted;
    private int totalBookingsTogether;
    private Instant lastBookedAt;
    private String nickname;
    private String notes;
    private HelperStatus currentStatus;
    private boolean availableForBooking;
}

