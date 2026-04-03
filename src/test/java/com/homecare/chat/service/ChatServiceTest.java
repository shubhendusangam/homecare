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
import com.homecare.core.enums.ServiceType;
import com.homecare.core.exception.BusinessException;
import com.homecare.core.exception.ResourceNotFoundException;
import com.homecare.notification.service.NotificationService;
import com.homecare.user.entity.User;
import com.homecare.user.enums.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ChatService")
class ChatServiceTest {

    @Mock private ChatMessageRepository chatMessageRepository;
    @Mock private BookingRepository bookingRepository;
    @Mock private NotificationService notificationService;
    @Mock private SimpMessagingTemplate messagingTemplate;
    @Mock private ApplicationEventPublisher eventPublisher;
    @Mock private ChatMessageResponseMapper chatMessageResponseMapper;

    @InjectMocks private ChatService chatService;

    private User customer;
    private User helper;
    private Booking booking;

    @BeforeEach
    void setUp() {
        customer = User.builder().name("Customer").email("c@test.com").role(Role.CUSTOMER).build();
        customer.setId(UUID.randomUUID());

        helper = User.builder().name("Helper").email("h@test.com").role(Role.HELPER).build();
        helper.setId(UUID.randomUUID());

        booking = Booking.builder()
                .customer(customer).helper(helper)
                .serviceType(ServiceType.CLEANING)
                .status(BookingStatus.ASSIGNED)
                .build();
        booking.setId(UUID.randomUUID());
    }

    @Nested
    @DisplayName("saveAndBroadcast")
    class SaveAndBroadcast {

        @Test
        @DisplayName("happy path — customer sends message on active booking")
        void customerSendsMessage() {
            when(bookingRepository.findById(booking.getId())).thenReturn(Optional.of(booking));
            when(chatMessageRepository.save(any(ChatMessage.class))).thenAnswer(inv -> {
                ChatMessage msg = inv.getArgument(0);
                msg.setId(UUID.randomUUID());
                msg.setCreatedAt(Instant.now());
                return msg;
            });
            ChatMessageResponse expectedDto = ChatMessageResponse.builder()
                    .id(UUID.randomUUID())
                    .bookingId(booking.getId())
                    .senderId(customer.getId())
                    .senderName(customer.getName())
                    .senderRole(SenderRole.CUSTOMER)
                    .content("Hello!")
                    .build();
            when(chatMessageResponseMapper.toDto(any())).thenReturn(expectedDto);

            ChatMessageResponse response = chatService.saveAndBroadcast(
                    booking.getId(), customer.getId(), "Hello!");

            assertNotNull(response);
            assertEquals(SenderRole.CUSTOMER, response.getSenderRole());
            verify(chatMessageRepository).save(any(ChatMessage.class));
            verify(messagingTemplate).convertAndSend(eq("/topic/chat/" + booking.getId()), any(ChatMessageResponse.class));
            verify(notificationService).sendToUser(eq(helper.getId()), any(), any());
            verify(eventPublisher).publishEvent(any());
        }

        @Test
        @DisplayName("happy path — helper sends message on active booking")
        void helperSendsMessage() {
            booking.setStatus(BookingStatus.IN_PROGRESS);
            when(bookingRepository.findById(booking.getId())).thenReturn(Optional.of(booking));
            when(chatMessageRepository.save(any(ChatMessage.class))).thenAnswer(inv -> {
                ChatMessage msg = inv.getArgument(0);
                msg.setId(UUID.randomUUID());
                msg.setCreatedAt(Instant.now());
                return msg;
            });
            ChatMessageResponse expectedDto = ChatMessageResponse.builder()
                    .id(UUID.randomUUID())
                    .senderRole(SenderRole.HELPER)
                    .content("On my way!")
                    .build();
            when(chatMessageResponseMapper.toDto(any())).thenReturn(expectedDto);

            ChatMessageResponse response = chatService.saveAndBroadcast(
                    booking.getId(), helper.getId(), "On my way!");

            assertNotNull(response);
            verify(chatMessageRepository).save(any(ChatMessage.class));
            verify(notificationService).sendToUser(eq(customer.getId()), any(), any());
        }

        @Test
        @DisplayName("COMPLETED booking → throws CHAT_NOT_ALLOWED")
        void completedBooking() {
            booking.setStatus(BookingStatus.COMPLETED);
            when(bookingRepository.findById(booking.getId())).thenReturn(Optional.of(booking));

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> chatService.saveAndBroadcast(booking.getId(), customer.getId(), "Hi"));
            assertEquals(ErrorCode.CHAT_NOT_ALLOWED, ex.getErrorCode());
        }

        @Test
        @DisplayName("CANCELLED booking → throws CHAT_NOT_ALLOWED")
        void cancelledBooking() {
            booking.setStatus(BookingStatus.CANCELLED);
            when(bookingRepository.findById(booking.getId())).thenReturn(Optional.of(booking));

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> chatService.saveAndBroadcast(booking.getId(), customer.getId(), "Hi"));
            assertEquals(ErrorCode.CHAT_NOT_ALLOWED, ex.getErrorCode());
        }

        @Test
        @DisplayName("PENDING_ASSIGNMENT booking → throws CHAT_NOT_ALLOWED")
        void pendingBooking() {
            booking.setStatus(BookingStatus.PENDING_ASSIGNMENT);
            when(bookingRepository.findById(booking.getId())).thenReturn(Optional.of(booking));

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> chatService.saveAndBroadcast(booking.getId(), customer.getId(), "Hi"));
            assertEquals(ErrorCode.CHAT_NOT_ALLOWED, ex.getErrorCode());
        }

        @Test
        @DisplayName("sender not a participant → throws CHAT_NOT_ALLOWED")
        void senderNotParticipant() {
            UUID outsider = UUID.randomUUID();
            when(bookingRepository.findById(booking.getId())).thenReturn(Optional.of(booking));

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> chatService.saveAndBroadcast(booking.getId(), outsider, "Hello!"));
            assertEquals(ErrorCode.CHAT_NOT_ALLOWED, ex.getErrorCode());
        }

        @Test
        @DisplayName("booking not found → throws ResourceNotFoundException")
        void bookingNotFound() {
            when(bookingRepository.findById(any())).thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class,
                    () -> chatService.saveAndBroadcast(UUID.randomUUID(), customer.getId(), "Hello!"));
        }
    }

    @Nested
    @DisplayName("getHistory")
    class GetHistory {

        @Test
        @DisplayName("happy path — returns paginated messages for participant")
        void happyPath() {
            ChatMessage msg = ChatMessage.builder()
                    .booking(booking).sender(customer).senderRole(SenderRole.CUSTOMER)
                    .content("Test").build();
            msg.setId(UUID.randomUUID());
            msg.setCreatedAt(Instant.now());

            Page<ChatMessage> page = new PageImpl<>(List.of(msg));
            when(bookingRepository.findById(booking.getId())).thenReturn(Optional.of(booking));
            when(chatMessageRepository.findByBookingIdOrderByCreatedAtDesc(eq(booking.getId()), any(Pageable.class)))
                    .thenReturn(page);
            when(chatMessageResponseMapper.toDto(any())).thenReturn(
                    ChatMessageResponse.builder().id(msg.getId()).content("Test").build());

            PagedResponse<ChatMessageResponse> result = chatService.getHistory(
                    booking.getId(), customer.getId(), PageRequest.of(0, 50));

            assertNotNull(result);
            assertEquals(1, result.getContent().size());
        }

        @Test
        @DisplayName("non-participant → throws FORBIDDEN")
        void nonParticipant() {
            UUID outsider = UUID.randomUUID();
            when(bookingRepository.findById(booking.getId())).thenReturn(Optional.of(booking));

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> chatService.getHistory(booking.getId(), outsider, PageRequest.of(0, 50)));
            assertEquals(ErrorCode.FORBIDDEN, ex.getErrorCode());
        }
    }

    @Nested
    @DisplayName("markAsRead")
    class MarkAsRead {

        @Test
        @DisplayName("happy path — marks messages and returns count")
        void happyPath() {
            when(bookingRepository.findById(booking.getId())).thenReturn(Optional.of(booking));
            when(chatMessageRepository.markAllAsReadForRecipient(
                    eq(booking.getId()), eq(customer.getId()), any(Instant.class))).thenReturn(3);

            int count = chatService.markAsRead(booking.getId(), customer.getId());

            assertEquals(3, count);
            verify(chatMessageRepository).markAllAsReadForRecipient(
                    eq(booking.getId()), eq(customer.getId()), any(Instant.class));
        }

        @Test
        @DisplayName("non-participant → throws FORBIDDEN")
        void nonParticipant() {
            UUID outsider = UUID.randomUUID();
            when(bookingRepository.findById(booking.getId())).thenReturn(Optional.of(booking));

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> chatService.markAsRead(booking.getId(), outsider));
            assertEquals(ErrorCode.FORBIDDEN, ex.getErrorCode());
        }
    }

    @Nested
    @DisplayName("getUnreadCount")
    class GetUnreadCount {

        @Test
        @DisplayName("returns correct unread count")
        void happyPath() {
            when(bookingRepository.findById(booking.getId())).thenReturn(Optional.of(booking));
            when(chatMessageRepository.countByBookingIdAndSenderIdNotAndReadByRecipientFalse(
                    booking.getId(), customer.getId())).thenReturn(5L);

            long count = chatService.getUnreadCount(booking.getId(), customer.getId());

            assertEquals(5, count);
        }
    }

    @Nested
    @DisplayName("purgeOldMessages")
    class PurgeOldMessages {

        @Test
        @DisplayName("deletes old records and returns count")
        void happyPath() {
            when(chatMessageRepository.deleteOlderThan(any(Instant.class))).thenReturn(42);

            int deleted = chatService.purgeOldMessages(90);

            assertEquals(42, deleted);
            verify(chatMessageRepository).deleteOlderThan(any(Instant.class));
        }
    }
}

