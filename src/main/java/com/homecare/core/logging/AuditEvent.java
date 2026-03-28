package com.homecare.core.logging;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Application-wide audit event for critical business actions.
 * Published via Spring's event system and handled asynchronously
 * by {@link AuditEventListener}.
 *
 * <pre>
 * publisher.publishEvent(AuditEvent.of("BOOKING_CREATED", userId, Map.of("bookingId", id)));
 * </pre>
 */
@Getter
public class AuditEvent extends ApplicationEvent {

    private final String action;
    private final UUID userId;
    private final Map<String, Object> details;
    private final Instant occurredAt;

    private AuditEvent(Object source, String action, UUID userId, Map<String, Object> details) {
        super(source);
        this.action = action;
        this.userId = userId;
        this.details = details != null ? details : Map.of();
        this.occurredAt = Instant.now();
    }

    /**
     * Factory method for publishing from any Spring bean.
     */
    public static AuditEvent of(String action, UUID userId, Map<String, Object> details) {
        return new AuditEvent("audit", action, userId, details);
    }

    public static AuditEvent of(String action, UUID userId) {
        return new AuditEvent("audit", action, userId, Map.of());
    }
}

