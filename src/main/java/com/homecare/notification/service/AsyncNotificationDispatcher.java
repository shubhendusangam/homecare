package com.homecare.notification.service;

import com.homecare.notification.email.EmailService;
import com.homecare.notification.entity.Notification;
import com.homecare.notification.repository.NotificationRepository;
import com.homecare.notification.sms.SmsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Dispatches email and SMS notifications asynchronously on the
 * {@code notificationExecutor} thread pool.
 * <p>
 * This is a separate Spring bean so that {@code @Async} AOP proxying works
 * correctly — same-class calls bypass the proxy. The calling
 * {@link NotificationServiceImpl} delegates slow I/O (SMTP, SMS HTTP) here,
 * keeping booking API response times under 100ms.
 * <p>
 * Delivery outcomes are tracked on the {@link Notification} entity
 * ({@code emailSentAt}, {@code emailFailed}, {@code smsSentAt}, {@code smsFailed}).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AsyncNotificationDispatcher {

    private final EmailService emailService;
    private final SmsService smsService;
    private final NotificationRepository notificationRepository;

    /**
     * Sends an HTML email asynchronously and updates delivery tracking.
     */
    @Async("notificationExecutor")
    public void sendEmail(UUID notificationId, String toEmail, String subject,
                          String templateName, Map<String, Object> templateVars) {
        try {
            emailService.sendHtmlEmail(toEmail, subject, templateName, templateVars);
            updateEmailStatus(notificationId, true);
        } catch (Exception e) {
            log.warn("Async email delivery failed: notificationId={}, to={}, error={}",
                    notificationId, toEmail, e.getMessage());
            updateEmailStatus(notificationId, false);
        }
    }

    /**
     * Sends an SMS asynchronously and updates delivery tracking.
     */
    @Async("notificationExecutor")
    public void sendSms(UUID notificationId, String phoneNumber, String message) {
        try {
            smsService.send(phoneNumber, message);
            updateSmsStatus(notificationId, true);
        } catch (Exception e) {
            log.warn("Async SMS delivery failed: notificationId={}, phone={}, error={}",
                    notificationId, phoneNumber, e.getMessage());
            updateSmsStatus(notificationId, false);
        }
    }

    // ─── Delivery tracking updates ─────────────────────────────────────

    private void updateEmailStatus(UUID notificationId, boolean success) {
        try {
            notificationRepository.findById(notificationId).ifPresent(n -> {
                if (success) {
                    n.setEmailSentAt(Instant.now());
                } else {
                    n.setEmailFailed(true);
                }
                notificationRepository.save(n);
            });
        } catch (Exception e) {
            log.warn("Failed to update email delivery status for notification {}: {}",
                    notificationId, e.getMessage());
        }
    }

    private void updateSmsStatus(UUID notificationId, boolean success) {
        try {
            notificationRepository.findById(notificationId).ifPresent(n -> {
                if (success) {
                    n.setSmsSentAt(Instant.now());
                } else {
                    n.setSmsFailed(true);
                }
                notificationRepository.save(n);
            });
        } catch (Exception e) {
            log.warn("Failed to update SMS delivery status for notification {}: {}",
                    notificationId, e.getMessage());
        }
    }
}

