package com.homecare.user.dto;

import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class CustomerProfileDto {
    private UUID userId;
    private String name;
    private String email;
    private String phone;
    private String avatarUrl;
    private String addressLine;
    private String city;
    private String state;
    private String pincode;
    private double latitude;
    private double longitude;
    private String preferredLanguage;
}

