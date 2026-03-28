package com.homecare.notification.sms;

/**
 * SMS delivery abstraction.
 * In prod, implement with Twilio / MSG91 / AWS SNS.
 */
public interface SmsService {

    /**
     * Sends an SMS to the given phone number.
     *
     * @param phoneNumber E.164 format phone number
     * @param message     SMS text
     */
    void send(String phoneNumber, String message);
}

