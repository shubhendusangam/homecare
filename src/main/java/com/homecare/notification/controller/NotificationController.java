package com.homecare.notification.controller;

import com.homecare.core.dto.ApiResponse;
import com.homecare.core.dto.PagedResponse;
import com.homecare.notification.dto.NotificationResponse;
import com.homecare.notification.dto.UnreadCountResponse;
import com.homecare.notification.service.NotificationQueryService;
import com.homecare.user.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationQueryService queryService;

    /**
     * GET /api/v1/notifications — own notifications (paginated, unread first)
     */
    @GetMapping
    public ResponseEntity<ApiResponse<PagedResponse<NotificationResponse>>> getNotifications(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        PagedResponse<NotificationResponse> response = queryService.getUserNotifications(
                principal.getId(), PageRequest.of(page, size));
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    /**
     * GET /api/v1/notifications/unread-count — returns { count: N }
     */
    @GetMapping("/unread-count")
    public ResponseEntity<ApiResponse<UnreadCountResponse>> getUnreadCount(
            @AuthenticationPrincipal UserPrincipal principal) {
        UnreadCountResponse response = queryService.getUnreadCount(principal.getId());
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    /**
     * PATCH /api/v1/notifications/{id}/read — mark single notification as read
     */
    @PatchMapping("/{id}/read")
    public ResponseEntity<ApiResponse<NotificationResponse>> markAsRead(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id) {
        NotificationResponse response = queryService.markAsRead(id, principal.getId());
        return ResponseEntity.ok(ApiResponse.ok(response, "Notification marked as read"));
    }

    /**
     * PATCH /api/v1/notifications/read-all — mark all as read
     */
    @PatchMapping("/read-all")
    public ResponseEntity<ApiResponse<Map<String, Integer>>> markAllAsRead(
            @AuthenticationPrincipal UserPrincipal principal) {
        int count = queryService.markAllAsRead(principal.getId());
        return ResponseEntity.ok(ApiResponse.ok(
                Map.of("markedRead", count),
                "All notifications marked as read"));
    }
}

