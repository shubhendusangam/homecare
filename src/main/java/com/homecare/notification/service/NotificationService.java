package com.homecare.notification.service;

import com.homecare.notification.enums.NotificationType;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Core notification service — sends notifications via all channels
 * (DB persistence, WebSocket push, email, SMS).
 */
public interface NotificationService {

    /**
     * Sends a notification to a single user via all channels.
     *
     * @param userId target user ID
     * @param type   notification type
     * @param vars   template variables (e.g., bookingId, helperName, amount)
     */
    void sendToUser(UUID userId, NotificationType type, Map<String, String> vars);

    /**
     * Sends a notification to multiple users.
     */
    void sendToUsers(List<UUID> userIds, NotificationType type, Map<String, String> vars);

    /**
     * Sends a system alert to all admin users.
     */
    void sendAdminAlert(String title, String body);
}

