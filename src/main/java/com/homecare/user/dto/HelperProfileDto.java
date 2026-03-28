package com.homecare.user.dto;

import com.homecare.core.enums.ServiceType;
import com.homecare.user.enums.HelperStatus;
import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
@Builder
public class HelperProfileDto {
    private UUID userId;
    private String name;
    private String email;
    private String phone;
    private String avatarUrl;
    private List<ServiceType> skills;
    private double latitude;
    private double longitude;
    private String city;
    private String pincode;
    private boolean available;
    private boolean backgroundVerified;
    private String idProofUrl;
    private double rating;
    private int totalJobsCompleted;
    private HelperStatus status;
}

