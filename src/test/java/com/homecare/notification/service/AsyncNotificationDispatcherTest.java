package com.homecare.notification.service;

import com.homecare.notification.email.EmailService;
import com.homecare.notification.entity.Notification;
import com.homecare.notification.repository.NotificationRepository;
import com.homecare.notification.sms.SmsService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.scheduling.annotation.Async;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AsyncNotificationDispatcher")
class AsyncNotificationDispatcherTest {

    @Mock private EmailService emailService;
    @Mock private SmsService smsService;
    @Mock private NotificationRepository notificationRepository;

    @InjectMocks private AsyncNotificationDispatcher dispatcher;

    @Nested
    @DisplayName("sendEmail")
    class SendEmail {

        @Test
        @DisplayName("is annotated with @Async('notificationExecutor')")
        void hasAsyncAnnotation() throws NoSuchMethodException {
            Method method = AsyncNotificationDispatcher.class.getMethod(
                    "sendEmail", UUID.class, String.class, String.class, String.class, Map.class);
            Async asyncAnnotation = method.getAnnotation(Async.class);

            assertNotNull(asyncAnnotation, "sendEmail should be annotated with @Async");
            assertEquals("notificationExecutor", asyncAnnotation.value(),
                    "@Async should use 'notificationExecutor' thread pool");
        }

        @Test
        @DisplayName("delegates to EmailService and updates delivery tracking on success")
        void successfulDelivery() {
            UUID notificationId = UUID.randomUUID();
            Notification notification = Notification.builder().build();
            notification.setId(notificationId);

            when(notificationRepository.findById(notificationId))
                    .thenReturn(Optional.of(notification));
            when(notificationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            dispatcher.sendEmail(notificationId, "user@test.com", "Subject",
                    "booking-confirmed", Map.of("key", "value"));

            verify(emailService).sendHtmlEmail(eq("user@test.com"), eq("Subject"),
                    eq("booking-confirmed"), any());
            verify(notificationRepository).save(argThat(n ->
                    n.getEmailSentAt() != null && !n.isEmailFailed()));
        }

        @Test
        @DisplayName("logs WARN and marks emailFailed on exception — does not rethrow")
        void failedDelivery() {
            UUID notificationId = UUID.randomUUID();
            Notification notification = Notification.builder().build();
            notification.setId(notificationId);

            doThrow(new RuntimeException("SMTP timeout"))
                    .when(emailService).sendHtmlEmail(any(), any(), any(), any());
            when(notificationRepository.findById(notificationId))
                    .thenReturn(Optional.of(notification));
            when(notificationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            // Should not throw — async failure must not propagate
            assertDoesNotThrow(() ->
                    dispatcher.sendEmail(notificationId, "user@test.com", "Subject",
                            "booking-confirmed", Map.of()));

            verify(notificationRepository).save(argThat(n ->
                    n.isEmailFailed() && n.getEmailSentAt() == null));
        }
    }

    @Nested
    @DisplayName("sendSms")
    class SendSms {

        @Test
        @DisplayName("is annotated with @Async('notificationExecutor')")
        void hasAsyncAnnotation() throws NoSuchMethodException {
            Method method = AsyncNotificationDispatcher.class.getMethod(
                    "sendSms", UUID.class, String.class, String.class);
            Async asyncAnnotation = method.getAnnotation(Async.class);

            assertNotNull(asyncAnnotation, "sendSms should be annotated with @Async");
            assertEquals("notificationExecutor", asyncAnnotation.value(),
                    "@Async should use 'notificationExecutor' thread pool");
        }

        @Test
        @DisplayName("delegates to SmsService and updates delivery tracking on success")
        void successfulDelivery() {
            UUID notificationId = UUID.randomUUID();
            Notification notification = Notification.builder().build();
            notification.setId(notificationId);

            when(notificationRepository.findById(notificationId))
                    .thenReturn(Optional.of(notification));
            when(notificationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            dispatcher.sendSms(notificationId, "9876543210", "Your helper is on the way");

            verify(smsService).send("9876543210", "Your helper is on the way");
            verify(notificationRepository).save(argThat(n ->
                    n.getSmsSentAt() != null && !n.isSmsFailed()));
        }

        @Test
        @DisplayName("logs WARN and marks smsFailed on exception — does not rethrow")
        void failedDelivery() {
            UUID notificationId = UUID.randomUUID();
            Notification notification = Notification.builder().build();
            notification.setId(notificationId);

            doThrow(new RuntimeException("SMS provider timeout"))
                    .when(smsService).send(any(), any());
            when(notificationRepository.findById(notificationId))
                    .thenReturn(Optional.of(notification));
            when(notificationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            // Should not throw
            assertDoesNotThrow(() ->
                    dispatcher.sendSms(notificationId, "9876543210", "Message"));

            verify(notificationRepository).save(argThat(n ->
                    n.isSmsFailed() && n.getSmsSentAt() == null));
        }
    }
}

