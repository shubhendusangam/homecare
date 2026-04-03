package com.homecare.notification.entity;

import com.homecare.core.entity.BaseEntity;
import com.homecare.notification.enums.NotificationType;
import com.homecare.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "notifications", indexes = {
        @Index(name = "idx_notification_user_read", columnList = "user_id, is_read"),
        @Index(name = "idx_notification_user_created", columnList = "user_id, created_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notification extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationType type;

    @Column(nullable = false)
    private String title;

    @Column(length = 2000, nullable = false)
    private String body;

    private String actionUrl;

    @Builder.Default
    @Column(name = "is_read", nullable = false)
    private boolean read = false;

    private Instant readAt;

    @Column(length = 2000)
    private String metadata;

    // ─── Delivery tracking ─────────────────────────────────────────────

    private Instant emailSentAt;

    @Builder.Default
    private boolean emailFailed = false;

    private Instant smsSentAt;

    @Builder.Default
    private boolean smsFailed = false;
}

