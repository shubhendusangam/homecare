package com.homecare.notification.dto;

import com.homecare.notification.enums.NotificationType;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Builder
public class NotificationResponse {
    private UUID id;
    private NotificationType type;
    private String title;
    private String body;
    private String actionUrl;
    private boolean read;
    private Instant readAt;
    private String metadata;
    private Instant createdAt;
}

