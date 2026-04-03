package com.homecare.chat.service;

import com.homecare.booking.entity.Booking;
import com.homecare.booking.repository.BookingRepository;
import com.homecare.chat.dto.ChatMessageResponse;
import com.homecare.chat.entity.ChatMessage;
import com.homecare.chat.enums.SenderRole;
import com.homecare.chat.mapper.ChatMessageResponseMapper;
import com.homecare.chat.repository.ChatMessageRepository;
import com.homecare.core.dto.PagedResponse;
import com.homecare.core.enums.BookingStatus;
import com.homecare.core.enums.ErrorCode;
import com.homecare.core.exception.BusinessException;
import com.homecare.core.exception.ResourceNotFoundException;
import com.homecare.core.logging.AuditEvent;
import com.homecare.notification.enums.NotificationType;
import com.homecare.notification.service.NotificationService;
import com.homecare.user.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatService {

    private final ChatMessageRepository chatMessageRepository;
    private final BookingRepository bookingRepository;
    private final NotificationService notificationService;
    private final SimpMessagingTemplate messagingTemplate;
    private final ApplicationEventPublisher eventPublisher;
    private final ChatMessageResponseMapper chatMessageResponseMapper;

    private static final Set<BookingStatus> CHAT_ALLOWED_STATUSES =
            Set.of(BookingStatus.ASSIGNED, BookingStatus.HELPER_EN_ROUTE, BookingStatus.IN_PROGRESS);

    // ─── Save & Broadcast ──────────────────────────────────────────────

    @Transactional
    public ChatMessageResponse saveAndBroadcast(UUID bookingId, UUID senderId, String content) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking", "id", bookingId));

        // Validate booking status
        if (!CHAT_ALLOWED_STATUSES.contains(booking.getStatus())) {
            throw new BusinessException(
                    "Chat is only available for active bookings. Current status: " + booking.getStatus(),
                    ErrorCode.CHAT_NOT_ALLOWED);
        }

        // Determine sender role and validate participation
        SenderRole senderRole;
        User recipient;
        if (booking.getCustomer().getId().equals(senderId)) {
            senderRole = SenderRole.CUSTOMER;
            recipient = booking.getHelper();
        } else if (booking.getHelper() != null && booking.getHelper().getId().equals(senderId)) {
            senderRole = SenderRole.HELPER;
            recipient = booking.getCustomer();
        } else {
            throw new BusinessException(
                    "You are not a participant in this booking",
                    ErrorCode.CHAT_NOT_ALLOWED);
        }

        // Persist message
        ChatMessage message = ChatMessage.builder()
                .booking(booking)
                .sender(senderRole == SenderRole.CUSTOMER ? booking.getCustomer() : booking.getHelper())
                .senderRole(senderRole)
                .content(content)
                .build();
        message = chatMessageRepository.save(message);

        ChatMessageResponse dto = chatMessageResponseMapper.toDto(message);

        // Broadcast to WebSocket topic
        messagingTemplate.convertAndSend("/topic/chat/" + bookingId, dto);

        // Send in-app notification to the other party
        try {
            notificationService.sendToUser(recipient.getId(), NotificationType.CHAT_MESSAGE,
                    Map.of("senderName", message.getSender().getName(),
                           "bookingId", bookingId.toString()));
        } catch (Exception e) {
            log.warn("Failed to send chat notification to user {}: {}", recipient.getId(), e.getMessage());
        }

        log.debug("Chat message saved: bookingId={}, senderId={}, role={}",
                bookingId, senderId, senderRole);
        eventPublisher.publishEvent(AuditEvent.of("CHAT_MESSAGE_SENT", senderId,
                Map.of("bookingId", bookingId, "recipientId", recipient.getId())));

        return dto;
    }

    // ─── Get History ───────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public PagedResponse<ChatMessageResponse> getHistory(UUID bookingId, UUID userId, Pageable pageable) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking", "id", bookingId));

        validateParticipant(booking, userId);

        Page<ChatMessageResponse> page = chatMessageRepository
                .findByBookingIdOrderByCreatedAtDesc(bookingId, pageable)
                .map(chatMessageResponseMapper::toDto);
        return PagedResponse.from(page);
    }

    /**
     * Admin variant — bypasses participant check for dispute resolution.
     */
    @Transactional(readOnly = true)
    public PagedResponse<ChatMessageResponse> getHistoryForAdmin(UUID bookingId, Pageable pageable) {
        bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking", "id", bookingId));

        Page<ChatMessageResponse> page = chatMessageRepository
                .findByBookingIdOrderByCreatedAtDesc(bookingId, pageable)
                .map(chatMessageResponseMapper::toDto);
        return PagedResponse.from(page);
    }

    // ─── Mark as Read ──────────────────────────────────────────────────

    @Transactional
    public int markAsRead(UUID bookingId, UUID userId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking", "id", bookingId));

        validateParticipant(booking, userId);

        int count = chatMessageRepository.markAllAsReadForRecipient(bookingId, userId, Instant.now());
        log.debug("Marked {} chat messages as read for user {} on booking {}", count, userId, bookingId);
        return count;
    }

    // ─── Unread Count ──────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public long getUnreadCount(UUID bookingId, UUID userId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking", "id", bookingId));

        validateParticipant(booking, userId);

        return chatMessageRepository.countByBookingIdAndSenderIdNotAndReadByRecipientFalse(bookingId, userId);
    }

    // ─── Cleanup old messages (90 days retention) ──────────────────────

    @Transactional
    public int purgeOldMessages(int retentionDays) {
        Instant cutoff = Instant.now().minus(retentionDays, ChronoUnit.DAYS);
        int deleted = chatMessageRepository.deleteOlderThan(cutoff);
        log.info("Purged {} chat messages older than {} days", deleted, retentionDays);
        return deleted;
    }

    // ─── Private Helpers ───────────────────────────────────────────────

    private void validateParticipant(Booking booking, UUID userId) {
        boolean isCustomer = booking.getCustomer().getId().equals(userId);
        boolean isHelper = booking.getHelper() != null && booking.getHelper().getId().equals(userId);
        if (!isCustomer && !isHelper) {
            throw new BusinessException(
                    "You are not a participant in this booking",
                    ErrorCode.FORBIDDEN);
        }
    }
}

