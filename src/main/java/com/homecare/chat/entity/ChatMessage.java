package com.homecare.chat.entity;

import com.homecare.booking.entity.Booking;
import com.homecare.chat.enums.SenderRole;
import com.homecare.core.entity.BaseEntity;
import com.homecare.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "chat_messages", indexes = {
        @Index(name = "idx_chat_booking_sent", columnList = "booking_id, created_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatMessage extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id", nullable = false)
    private Booking booking;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_id", nullable = false)
    private User sender;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SenderRole senderRole;

    @Column(nullable = false, length = 1000)
    private String content;

    @Builder.Default
    private boolean readByRecipient = false;

    private Instant readAt;
}

