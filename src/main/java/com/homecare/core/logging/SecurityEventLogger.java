package com.homecare.core.logging;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AuthenticationFailureBadCredentialsEvent;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.stereotype.Component;

/**
 * Listens for Spring Security authentication events and writes them
 * to the {@code AUDIT} logger for security observability.
 * <p>
 * Captures:
 * <ul>
 *   <li>Authentication failures (bad credentials, locked accounts)</li>
 *   <li>Successful authentications (optional, at DEBUG)</li>
 * </ul>
 */
@Component
@Slf4j
public class SecurityEventLogger {

    private static final Logger AUDIT_LOG = LoggerFactory.getLogger("AUDIT");

    @EventListener
    public void onAuthFailure(AuthenticationFailureBadCredentialsEvent event) {
        String principal = event.getAuthentication().getName();
        String exType = event.getException().getClass().getSimpleName();
        String ip = MDC.get("clientIp");
        String requestId = MDC.get("requestId");

        AUDIT_LOG.warn("SECURITY | event=AUTH_FAILURE | principal={} | reason={} | ip={} | requestId={}",
                principal, exType, ip != null ? ip : "unknown", requestId);
    }

    @EventListener
    public void onAuthSuccess(AuthenticationSuccessEvent event) {
        if (log.isDebugEnabled()) {
            String principal = event.getAuthentication().getName();
            log.debug("SECURITY | event=AUTH_SUCCESS | principal={}", principal);
        }
    }
}

