package com.homecare.notification.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.homecare.core.exception.ResourceNotFoundException;
import com.homecare.notification.config.NotificationTemplates;
import com.homecare.notification.dto.NotificationResponse;
import com.homecare.notification.email.EmailService;
import com.homecare.notification.entity.Notification;
import com.homecare.notification.enums.NotificationType;
import com.homecare.notification.repository.NotificationRepository;
import com.homecare.notification.sms.SmsService;
import com.homecare.notification.websocket.WebSocketNotificationPusher;
import com.homecare.user.entity.User;
import com.homecare.user.enums.Role;
import com.homecare.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final NotificationTemplates templates;
    private final WebSocketNotificationPusher webSocketPusher;
    private final EmailService emailService;
    private final SmsService smsService;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public void sendToUser(UUID userId, NotificationType type, Map<String, String> vars) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        Map<String, String> resolvedVars = vars != null ? vars : Map.of();

        // 1. Resolve template
        String title = templates.resolve(templates.getTitle(type), resolvedVars);
        String body = templates.resolve(templates.getBody(type), resolvedVars);
        String actionUrl = templates.resolve(templates.getActionUrl(type), resolvedVars);
        String metadata = serializeMetadata(resolvedVars);

        // 2. Persist to DB
        Notification notification = Notification.builder()
                .user(user)
                .type(type)
                .title(title)
                .body(body)
                .actionUrl(actionUrl)
                .read(false)
                .metadata(metadata)
                .build();
        notification = notificationRepository.save(notification);

        NotificationResponse response = toDto(notification);

        // 3. WebSocket push
        webSocketPusher.push(userId, response);

        // 4. Email (async, only for certain types)
        sendEmailIfApplicable(user, type, resolvedVars, title);

        // 5. SMS (for high-priority notifications)
        sendSmsIfApplicable(user, type, body);

        log.info("Notification sent: type={}, userId={}, title='{}'", type, userId, title);
    }

    @Override
    @Transactional
    public void sendToUsers(List<UUID> userIds, NotificationType type, Map<String, String> vars) {
        for (UUID userId : userIds) {
            try {
                sendToUser(userId, type, vars);
            } catch (Exception e) {
                log.error("Failed to send notification to user {}: {}", userId, e.getMessage());
            }
        }
    }

    @Override
    @Transactional
    public void sendAdminAlert(String title, String body) {
        List<User> admins = userRepository.findAllWithFilters(
                Role.ADMIN, true, null, PageRequest.of(0, 100)).getContent();

        Map<String, String> vars = Map.of("title", title, "body", body);

        for (User admin : admins) {
            try {
                sendToUser(admin.getId(), NotificationType.SYSTEM_ALERT, vars);
            } catch (Exception e) {
                log.error("Failed to send admin alert to {}: {}", admin.getId(), e.getMessage());
            }
        }
    }

    // ─── Email dispatch ───────────────────────────────────────────────

    private void sendEmailIfApplicable(User user, NotificationType type,
                                       Map<String, String> vars, String subject) {
        String templateName = templates.getEmailTemplateName(type);
        if (templateName == null) return;

        Map<String, Object> emailModel = new HashMap<>(vars);
        emailModel.put("userName", user.getName() != null ? user.getName() : "User");
        emailModel.put("userEmail", user.getEmail());

        emailService.sendHtmlEmail(user.getEmail(), subject, templateName, emailModel);
    }

    // ─── SMS dispatch ─────────────────────────────────────────────────

    private void sendSmsIfApplicable(User user, NotificationType type, String body) {
        // Only send SMS for high-priority notification types
        if (user.getPhone() == null) return;

        boolean highPriority = switch (type) {
            case BOOKING_ASSIGNED, HELPER_EN_ROUTE, BOOKING_CANCELLED, BOOKING_REMINDER -> true;
            default -> false;
        };

        if (highPriority) {
            smsService.send(user.getPhone(), body);
        }
    }

    // ─── Helpers ──────────────────────────────────────────────────────

    private String serializeMetadata(Map<String, String> vars) {
        try {
            return objectMapper.writeValueAsString(vars);
        } catch (JsonProcessingException e) {
            log.warn("Failed to serialize notification metadata: {}", e.getMessage());
            return "{}";
        }
    }

    private NotificationResponse toDto(Notification n) {
        return NotificationResponse.builder()
                .id(n.getId())
                .type(n.getType())
                .title(n.getTitle())
                .body(n.getBody())
                .actionUrl(n.getActionUrl())
                .read(n.isRead())
                .readAt(n.getReadAt())
                .metadata(n.getMetadata())
                .createdAt(n.getCreatedAt())
                .build();
    }
}

