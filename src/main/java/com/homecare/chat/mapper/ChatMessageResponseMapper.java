package com.homecare.chat.mapper;

import com.homecare.chat.dto.ChatMessageResponse;
import com.homecare.chat.entity.ChatMessage;
import org.springframework.stereotype.Component;

/**
 * Centralised mapper for {@link ChatMessage} → {@link ChatMessageResponse}.
 */
@Component
public class ChatMessageResponseMapper {

    public ChatMessageResponse toDto(ChatMessage msg) {
        return ChatMessageResponse.builder()
                .id(msg.getId())
                .bookingId(msg.getBooking().getId())
                .senderId(msg.getSender().getId())
                .senderName(msg.getSender().getName())
                .senderRole(msg.getSenderRole())
                .content(msg.getContent())
                .readByRecipient(msg.isReadByRecipient())
                .readAt(msg.getReadAt())
                .sentAt(msg.getCreatedAt())
                .build();
    }
}

