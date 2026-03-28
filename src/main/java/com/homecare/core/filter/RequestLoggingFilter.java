package com.homecare.core.filter;

import com.homecare.user.security.UserPrincipal;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * Centralized request logging filter. Populates MDC with:
 * <ul>
 *   <li><b>requestId</b> — unique per-request correlation ID</li>
 *   <li><b>clientIp</b>  — client IP (respects X-Forwarded-For)</li>
 *   <li><b>method</b>    — HTTP method</li>
 *   <li><b>uri</b>       — request URI</li>
 *   <li><b>userId</b>    — authenticated user ID (set after security chain)</li>
 *   <li><b>role</b>      — authenticated user role (set after security chain)</li>
 * </ul>
 * All downstream loggers automatically include this context.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
@Slf4j
public class RequestLoggingFilter extends OncePerRequestFilter {

    private static final String REQUEST_ID_HEADER = "X-Request-Id";
    private static final String MDC_REQUEST_ID = "requestId";
    private static final String MDC_USER_ID = "userId";
    private static final String MDC_ROLE = "role";
    private static final String MDC_CLIENT_IP = "clientIp";
    private static final String MDC_METHOD = "method";
    private static final String MDC_URI = "uri";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        // ── Generate / propagate request ID ────────────────────────────
        String requestId = request.getHeader(REQUEST_ID_HEADER);
        if (requestId == null || requestId.isBlank()) {
            requestId = UUID.randomUUID().toString();
        }

        // ── Populate MDC with request context ─────────────────────────
        MDC.put(MDC_REQUEST_ID, requestId);
        MDC.put(MDC_CLIENT_IP, resolveClientIp(request));
        MDC.put(MDC_METHOD, request.getMethod());
        MDC.put(MDC_URI, request.getRequestURI());

        response.setHeader(REQUEST_ID_HEADER, requestId);

        String method = request.getMethod();
        String path = request.getRequestURI();
        String queryString = request.getQueryString();
        String fullPath = queryString != null ? path + "?" + queryString : path;
        long start = System.currentTimeMillis();

        log.info("→ {} {}", method, fullPath);

        try {
            filterChain.doFilter(request, response);
        } finally {
            long elapsed = System.currentTimeMillis() - start;

            // ── Enrich MDC with user context (available after security filter chain) ─
            populateUserContext();

            int status = response.getStatus();
            if (status >= 500) {
                log.error("← {} {} {} {}ms", method, fullPath, status, elapsed);
            } else if (status >= 400) {
                log.warn("← {} {} {} {}ms", method, fullPath, status, elapsed);
            } else {
                log.info("← {} {} {} {}ms", method, fullPath, status, elapsed);
            }

            // ── Clear MDC — prevent leaking between pooled threads ────
            MDC.remove(MDC_REQUEST_ID);
            MDC.remove(MDC_USER_ID);
            MDC.remove(MDC_ROLE);
            MDC.remove(MDC_CLIENT_IP);
            MDC.remove(MDC_METHOD);
            MDC.remove(MDC_URI);
        }
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.startsWith("/css/") || path.startsWith("/js/") ||
               path.startsWith("/images/") || path.endsWith(".ico");
    }

    /**
     * Extract user identity from SecurityContext and push into MDC.
     * Called after the rest of the filter chain has executed so that
     * the JWT filter has already populated the SecurityContext.
     */
    private void populateUserContext() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated() && auth.getPrincipal() instanceof UserPrincipal principal) {
                MDC.put(MDC_USER_ID, principal.getId().toString());
                MDC.put(MDC_ROLE, principal.getRole().name());
            }
        } catch (Exception ignored) {
            // Security context not available — leave MDC fields empty
        }
    }

    /**
     * Resolve the real client IP, respecting reverse-proxy headers.
     */
    private String resolveClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            // X-Forwarded-For can be comma-separated; first value is the original client
            return forwarded.split(",")[0].trim();
        }
        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) {
            return realIp;
        }
        return request.getRemoteAddr();
    }
}

