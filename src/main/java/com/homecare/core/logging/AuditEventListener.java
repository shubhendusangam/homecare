package com.homecare.core.logging;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * Asynchronously handles {@link AuditEvent}s and writes them to the
 * dedicated {@code AUDIT} logger. In production the logback-spring.xml
 * routes this logger to a separate file / JSON stream for compliance.
 */
@Component
@Slf4j
public class AuditEventListener {

    /**
     * Dedicated logger — allows logback to route audit entries to a
     * separate appender (file, Kafka, database) via logger name.
     */
    private static final org.slf4j.Logger AUDIT_LOG =
            org.slf4j.LoggerFactory.getLogger("AUDIT");

    @Async
    @EventListener
    public void onAuditEvent(AuditEvent event) {
        AUDIT_LOG.info("AUDIT | action={} | userId={} | details={} | at={}",
                event.getAction(),
                event.getUserId(),
                event.getDetails(),
                event.getOccurredAt());
    }
}

