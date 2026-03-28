package com.homecare.user.dto;

import com.homecare.user.enums.Role;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
public class UserAdminDto {
    private UUID id;
    private String name;
    private String email;
    private String phone;
    private Role role;
    private boolean active;
    private boolean emailVerified;
    private boolean phoneVerified;
    private Instant lastLoginAt;
    private Instant createdAt;
}

