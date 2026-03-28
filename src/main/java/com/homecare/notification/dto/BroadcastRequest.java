package com.homecare.notification.dto;

import com.homecare.user.enums.Role;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
public class BroadcastRequest {

    @NotBlank(message = "Title is required")
    private String title;

    @NotBlank(message = "Body is required")
    private String body;

    /**
     * Optional list of specific user IDs. If null/empty, targets by role.
     */
    private List<UUID> userIds;

    /**
     * Target role (CUSTOMER, HELPER). Used when userIds is empty.
     */
    private Role role;
}

