package com.homecare.chat.dto;

import com.homecare.chat.enums.SenderRole;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Builder
public class ChatMessageResponse {
    private UUID id;
    private UUID bookingId;
    private UUID senderId;
    private String senderName;
    private SenderRole senderRole;
    private String content;
    private boolean readByRecipient;
    private Instant readAt;
    private Instant sentAt;
}

