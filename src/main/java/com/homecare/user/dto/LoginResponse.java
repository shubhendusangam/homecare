package com.homecare.user.dto;

import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class LoginResponse {
    private String accessToken;
    private String refreshToken;
    private String role;
    private UUID userId;
    private String name;
    private String email;
}

