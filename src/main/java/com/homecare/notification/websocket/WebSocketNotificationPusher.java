package com.homecare.notification.websocket;

import com.homecare.notification.dto.NotificationResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Pushes notification payloads to connected WebSocket clients.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class WebSocketNotificationPusher {

    private final SimpMessagingTemplate messagingTemplate;

    /**
     * Sends a notification to a specific user's notification queue.
     */
    public void push(UUID userId, NotificationResponse notification) {
        try {
            messagingTemplate.convertAndSendToUser(
                    userId.toString(),
                    "/queue/notifications",
                    notification
            );
            log.debug("WebSocket notification pushed to user {}: {}", userId, notification.getType());
        } catch (Exception e) {
            log.warn("Failed to push WebSocket notification to user {}: {}", userId, e.getMessage());
        }
    }
}

