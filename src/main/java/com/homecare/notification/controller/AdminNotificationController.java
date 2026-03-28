package com.homecare.notification.controller;

import com.homecare.core.dto.ApiResponse;
import com.homecare.notification.dto.BroadcastRequest;
import com.homecare.notification.enums.NotificationType;
import com.homecare.notification.service.NotificationService;
import com.homecare.user.entity.User;
import com.homecare.user.repository.UserRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/notifications")
@RequiredArgsConstructor
public class AdminNotificationController {

    private final NotificationService notificationService;
    private final UserRepository userRepository;

    /**
     * POST /api/v1/admin/notifications/broadcast
     * Broadcasts a notification to specific users or all users of a given role.
     */
    @PostMapping("/broadcast")
    public ResponseEntity<ApiResponse<Map<String, Object>>> broadcast(
            @Valid @RequestBody BroadcastRequest request) {

        List<UUID> targetUserIds;

        if (request.getUserIds() != null && !request.getUserIds().isEmpty()) {
            targetUserIds = request.getUserIds();
        } else if (request.getRole() != null) {
            targetUserIds = userRepository.findAllWithFilters(
                    request.getRole(), true, null, PageRequest.of(0, 10000))
                    .getContent()
                    .stream()
                    .map(User::getId)
                    .toList();
        } else {
            // Broadcast to ALL active users
            targetUserIds = userRepository.findAllWithFilters(
                    null, true, null, PageRequest.of(0, 10000))
                    .getContent()
                    .stream()
                    .map(User::getId)
                    .toList();
        }

        Map<String, String> vars = Map.of(
                "title", request.getTitle(),
                "body", request.getBody()
        );

        notificationService.sendToUsers(targetUserIds, NotificationType.SYSTEM_ALERT, vars);

        return ResponseEntity.ok(ApiResponse.ok(
                Map.of("recipientCount", targetUserIds.size(),
                       "title", request.getTitle()),
                "Broadcast sent successfully"));
    }
}

