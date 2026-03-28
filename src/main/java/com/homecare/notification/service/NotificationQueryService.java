package com.homecare.notification.service;

import com.homecare.core.dto.PagedResponse;
import com.homecare.core.enums.ErrorCode;
import com.homecare.core.exception.BusinessException;
import com.homecare.core.exception.ResourceNotFoundException;
import com.homecare.notification.dto.NotificationResponse;
import com.homecare.notification.dto.UnreadCountResponse;
import com.homecare.notification.entity.Notification;
import com.homecare.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationQueryService {

    private final NotificationRepository notificationRepository;

    // ─── Get Notifications (paginated, unread first) ──────────────────

    @Transactional(readOnly = true)
    public PagedResponse<NotificationResponse> getUserNotifications(UUID userId, Pageable pageable) {
        Page<NotificationResponse> page = notificationRepository
                .findByUserIdOrderByReadAscCreatedAtDesc(userId, pageable)
                .map(this::toDto);
        return PagedResponse.from(page);
    }

    // ─── Get Unread Count ─────────────────────────────────────────────

    @Transactional(readOnly = true)
    public UnreadCountResponse getUnreadCount(UUID userId) {
        long count = notificationRepository.countByUserIdAndReadFalse(userId);
        return UnreadCountResponse.builder().count(count).build();
    }

    // ─── Mark Single as Read ──────────────────────────────────────────

    @Transactional
    public NotificationResponse markAsRead(UUID notificationId, UUID userId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification", "id", notificationId));

        if (!notification.getUser().getId().equals(userId)) {
            throw new BusinessException("You can only read your own notifications", ErrorCode.FORBIDDEN);
        }

        if (!notification.isRead()) {
            notification.setRead(true);
            notification.setReadAt(Instant.now());
            notificationRepository.save(notification);
        }

        return toDto(notification);
    }

    // ─── Mark All as Read ─────────────────────────────────────────────

    @Transactional
    public int markAllAsRead(UUID userId) {
        int count = notificationRepository.markAllAsRead(userId, Instant.now());
        log.info("Marked {} notifications as read for user {}", count, userId);
        return count;
    }

    // ─── DTO Mapping ──────────────────────────────────────────────────

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

