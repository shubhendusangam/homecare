package com.homecare.notification.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.homecare.core.exception.ResourceNotFoundException;
import com.homecare.notification.config.NotificationTemplates;
import com.homecare.notification.dto.NotificationResponse;
import com.homecare.notification.entity.Notification;
import com.homecare.notification.enums.NotificationType;
import com.homecare.notification.repository.NotificationRepository;
import com.homecare.notification.websocket.WebSocketNotificationPusher;
import com.homecare.user.entity.User;
import com.homecare.user.enums.Role;
import com.homecare.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("NotificationServiceImpl")
class NotificationServiceImplTest {

    @Mock private NotificationRepository notificationRepository;
    @Mock private UserRepository userRepository;
    @Spy private NotificationTemplates templates;
    @Mock private WebSocketNotificationPusher webSocketPusher;
    @Mock private AsyncNotificationDispatcher asyncDispatcher;
    @Spy private ObjectMapper objectMapper;

    @InjectMocks private NotificationServiceImpl notificationService;

    private User customer;
    private User admin;

    @BeforeEach
    void setUp() {
        customer = User.builder()
                .name("John").email("john@test.com").phone("9876543210")
                .role(Role.CUSTOMER).active(true).build();
        customer.setId(UUID.randomUUID());

        admin = User.builder()
                .name("Admin").email("admin@test.com")
                .role(Role.ADMIN).active(true).build();
        admin.setId(UUID.randomUUID());
    }

    @Nested
    @DisplayName("sendToUser")
    class SendToUser {

        @Test
        @DisplayName("happy path — persists, pushes WebSocket, dispatches email async")
        void happyPath() {
            when(userRepository.findById(customer.getId())).thenReturn(Optional.of(customer));
            when(notificationRepository.save(any(Notification.class))).thenAnswer(inv -> {
                Notification n = inv.getArgument(0);
                n.setId(UUID.randomUUID());
                return n;
            });

            Map<String, String> vars = Map.of("bookingId", UUID.randomUUID().toString(),
                    "serviceType", "CLEANING");

            notificationService.sendToUser(customer.getId(),
                    NotificationType.BOOKING_CONFIRMED, vars);

            verify(notificationRepository).save(any(Notification.class));
            verify(webSocketPusher).push(eq(customer.getId()), any(NotificationResponse.class));
            // Email dispatched async via dispatcher (BOOKING_CONFIRMED has email template)
            verify(asyncDispatcher).sendEmail(any(UUID.class), eq("john@test.com"),
                    anyString(), eq("booking-confirmed"), any());
        }

        @Test
        @DisplayName("user not found → throws")
        void userNotFound() {
            when(userRepository.findById(any())).thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class,
                    () -> notificationService.sendToUser(UUID.randomUUID(),
                            NotificationType.BOOKING_CONFIRMED, Map.of()));
        }

        @Test
        @DisplayName("SMS dispatched async for high-priority types (BOOKING_ASSIGNED)")
        void smsSentForHighPriority() {
            when(userRepository.findById(customer.getId())).thenReturn(Optional.of(customer));
            when(notificationRepository.save(any())).thenAnswer(inv -> {
                Notification n = inv.getArgument(0);
                n.setId(UUID.randomUUID());
                return n;
            });

            notificationService.sendToUser(customer.getId(),
                    NotificationType.BOOKING_ASSIGNED, Map.of());

            verify(asyncDispatcher).sendSms(any(UUID.class), eq("9876543210"), anyString());
        }

        @Test
        @DisplayName("SMS NOT dispatched for low-priority types (PAYMENT_SUCCESS)")
        void smsNotSentForLowPriority() {
            when(userRepository.findById(customer.getId())).thenReturn(Optional.of(customer));
            when(notificationRepository.save(any())).thenAnswer(inv -> {
                Notification n = inv.getArgument(0);
                n.setId(UUID.randomUUID());
                return n;
            });

            notificationService.sendToUser(customer.getId(),
                    NotificationType.PAYMENT_SUCCESS, Map.of());

            verify(asyncDispatcher, never()).sendSms(any(), anyString(), anyString());
        }

        @Test
        @DisplayName("null vars defaults to empty map")
        void nullVars() {
            when(userRepository.findById(customer.getId())).thenReturn(Optional.of(customer));
            when(notificationRepository.save(any())).thenAnswer(inv -> {
                Notification n = inv.getArgument(0);
                n.setId(UUID.randomUUID());
                return n;
            });

            assertDoesNotThrow(() ->
                    notificationService.sendToUser(customer.getId(),
                            NotificationType.BOOKING_CONFIRMED, null));
        }
    }

    @Nested
    @DisplayName("sendToUsers")
    class SendToUsers {

        @Test
        @DisplayName("sends to multiple users and handles individual failures gracefully")
        void sendsToMultiple() {
            User user2 = User.builder().name("Jane").email("jane@test.com")
                    .role(Role.CUSTOMER).active(true).build();
            user2.setId(UUID.randomUUID());

            when(userRepository.findById(customer.getId())).thenReturn(Optional.of(customer));
            when(userRepository.findById(user2.getId()))
                    .thenThrow(new ResourceNotFoundException("User", "id", user2.getId()));
            when(notificationRepository.save(any())).thenAnswer(inv -> {
                Notification n = inv.getArgument(0);
                n.setId(UUID.randomUUID());
                return n;
            });

            // Should not throw even though user2 fails
            assertDoesNotThrow(() ->
                    notificationService.sendToUsers(
                            List.of(customer.getId(), user2.getId()),
                            NotificationType.BOOKING_CONFIRMED, Map.of()));

            // First user should still get notification
            verify(webSocketPusher).push(eq(customer.getId()), any());
        }
    }

    @Nested
    @DisplayName("sendAdminAlert")
    class SendAdminAlert {

        @Test
        @DisplayName("sends to all active admins")
        void sendsToAllAdmins() {
            when(userRepository.findAllWithFilters(eq(Role.ADMIN), eq(true), isNull(), any()))
                    .thenReturn(new PageImpl<>(List.of(admin)));
            when(userRepository.findById(admin.getId())).thenReturn(Optional.of(admin));
            when(notificationRepository.save(any())).thenAnswer(inv -> {
                Notification n = inv.getArgument(0);
                n.setId(UUID.randomUUID());
                return n;
            });

            notificationService.sendAdminAlert("Test Alert", "Something happened");

            verify(webSocketPusher).push(eq(admin.getId()), any());
        }
    }
}

