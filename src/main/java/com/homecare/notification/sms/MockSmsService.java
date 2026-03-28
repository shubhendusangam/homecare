package com.homecare.notification.sms;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

/**
 * Mock SMS service for dev/test environments.
 * Logs the message to console without sending.
 */
@Service
@Profile({"dev", "test"})
@Slf4j
public class MockSmsService implements SmsService {

    @Override
    public void send(String phoneNumber, String message) {
        log.info("═══ MOCK SMS ═══");
        log.info("  To:      {}", phoneNumber);
        log.info("  Message: {}", message);
        log.info("════════════════");
    }
}

