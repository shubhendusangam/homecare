package com.homecare.notification.config;

import com.homecare.notification.enums.NotificationType;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.Map;

/**
 * Holds title/body templates for each notification type.
 * Variables in {{curly braces}} are replaced at send time.
 */
@Component
public class NotificationTemplates {

    private final Map<NotificationType, String[]> templates = new EnumMap<>(NotificationType.class);

    public NotificationTemplates() {
        // { title, body, actionUrl pattern }
        templates.put(NotificationType.BOOKING_CONFIRMED, new String[]{
                "Booking Confirmed",
                "Your {{serviceType}} booking #{{bookingId}} has been placed successfully. We're finding a helper for you.",
                "/bookings/{{bookingId}}"
        });
        templates.put(NotificationType.BOOKING_ASSIGNED, new String[]{
                "Helper Assigned",
                "{{helperName}} has been assigned to your {{serviceType}} booking. They will arrive at the scheduled time.",
                "/bookings/{{bookingId}}"
        });
        templates.put(NotificationType.HELPER_EN_ROUTE, new String[]{
                "Helper On The Way",
                "{{helperName}} is on their way to your location for {{serviceType}} service.",
                "/bookings/{{bookingId}}/track"
        });
        templates.put(NotificationType.BOOKING_COMPLETED, new String[]{
                "Booking Completed",
                "Your {{serviceType}} booking has been completed. Please rate your experience.",
                "/bookings/{{bookingId}}/review"
        });
        templates.put(NotificationType.BOOKING_CANCELLED, new String[]{
                "Booking Cancelled",
                "Your {{serviceType}} booking #{{bookingId}} has been cancelled. {{reason}}",
                "/bookings/{{bookingId}}"
        });
        templates.put(NotificationType.BOOKING_REMINDER, new String[]{
                "Booking Reminder",
                "Your {{serviceType}} booking is scheduled in 30 minutes at {{address}}.",
                "/bookings/{{bookingId}}"
        });
        templates.put(NotificationType.PAYMENT_SUCCESS, new String[]{
                "Payment Successful",
                "₹{{amount}} has been credited to your wallet. Current balance: ₹{{balance}}.",
                "/wallet"
        });
        templates.put(NotificationType.PAYMENT_REFUND, new String[]{
                "Refund Processed",
                "₹{{amount}} has been refunded for booking #{{bookingId}}.",
                "/wallet/transactions"
        });
        templates.put(NotificationType.NEW_REVIEW, new String[]{
                "New Review Received",
                "You received a {{rating}}-star review for your {{serviceType}} service.",
                "/reviews"
        });
        templates.put(NotificationType.WALLET_LOW, new String[]{
                "Low Wallet Balance",
                "Your wallet balance is ₹{{balance}}. Top up to continue booking services.",
                "/wallet/topup"
        });
        templates.put(NotificationType.SYSTEM_ALERT, new String[]{
                "{{title}}",
                "{{body}}",
                ""
        });
    }

    public String getTitle(NotificationType type) {
        String[] t = templates.get(type);
        return t != null ? t[0] : "Notification";
    }

    public String getBody(NotificationType type) {
        String[] t = templates.get(type);
        return t != null ? t[1] : "";
    }

    public String getActionUrl(NotificationType type) {
        String[] t = templates.get(type);
        return t != null ? t[2] : "";
    }

    /**
     * Resolves {{variable}} placeholders in a template string.
     */
    public String resolve(String template, Map<String, String> vars) {
        if (template == null || vars == null) return template;
        String result = template;
        for (Map.Entry<String, String> entry : vars.entrySet()) {
            result = result.replace("{{" + entry.getKey() + "}}", entry.getValue());
        }
        return result;
    }

    /**
     * Returns the email template name for a notification type, or null if no email should be sent.
     */
    public String getEmailTemplateName(NotificationType type) {
        return switch (type) {
            case BOOKING_CONFIRMED -> "booking-confirmed";
            case BOOKING_ASSIGNED -> "booking-assigned";
            case BOOKING_COMPLETED -> "booking-completed";
            case BOOKING_CANCELLED -> "booking-cancelled";
            case PAYMENT_SUCCESS -> "payment-receipt";
            case PAYMENT_REFUND -> "payment-refund";
            case BOOKING_REMINDER -> "booking-reminder";
            default -> null; // no email for other types
        };
    }
}

