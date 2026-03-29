package com.homecare.core.filter;

import com.homecare.core.logging.SensitiveDataMasker;
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
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.UUID;

/**
 * Centralized request logging filter. Populates MDC with:
 * <ul>
 *   <li><b>requestId</b>    — unique per-request correlation ID</li>
 *   <li><b>clientIp</b>     — client IP (respects X-Forwarded-For)</li>
 *   <li><b>method</b>       — HTTP method</li>
 *   <li><b>uri</b>          — request URI</li>
 *   <li><b>userId</b>       — authenticated user ID (set after security chain)</li>
 *   <li><b>role</b>         — authenticated user role (set after security chain)</li>
 *   <li><b>userAgent</b>    — User-Agent header</li>
 *   <li><b>responseStatus</b> — HTTP response status code</li>
 *   <li><b>contentLength</b>  — response content length</li>
 * </ul>
 * All downstream loggers automatically include this context.
 * <p>
 * Optionally logs request/response bodies at DEBUG level with sensitive data masking.
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
    private static final String MDC_USER_AGENT = "userAgent";
    private static final String MDC_RESPONSE_STATUS = "responseStatus";
    private static final String MDC_CONTENT_LENGTH = "contentLength";

    /** Maximum request/response body size to log (bytes). */
    private static final int MAX_BODY_LOG_SIZE = 2048;

    /** Content types eligible for body logging. */
    private static final Set<String> LOGGABLE_CONTENT_TYPES = Set.of(
            "application/json", "application/xml", "text/plain", "text/xml"
    );

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
        MDC.put(MDC_USER_AGENT, truncate(request.getHeader("User-Agent"), 150));

        response.setHeader(REQUEST_ID_HEADER, requestId);

        // ── Wrap request/response for body caching ────────────────────
        ContentCachingRequestWrapper wrappedRequest = new ContentCachingRequestWrapper(request, MAX_BODY_LOG_SIZE);
        ContentCachingResponseWrapper wrappedResponse = new ContentCachingResponseWrapper(response);

        String method = request.getMethod();
        String path = request.getRequestURI();
        String queryString = request.getQueryString();
        String fullPath = queryString != null ? path + "?" + queryString : path;
        long start = System.currentTimeMillis();

        log.info("→ {} {}", method, fullPath);

        try {
            filterChain.doFilter(wrappedRequest, wrappedResponse);
        } finally {
            long elapsed = System.currentTimeMillis() - start;

            // ── Enrich MDC with user context (available after security filter chain) ─
            populateUserContext();

            int status = wrappedResponse.getStatus();
            MDC.put(MDC_RESPONSE_STATUS, String.valueOf(status));

            String contentLen = wrappedResponse.getHeader("Content-Length");
            if (contentLen == null) {
                contentLen = String.valueOf(wrappedResponse.getContentSize());
            }
            MDC.put(MDC_CONTENT_LENGTH, contentLen);

            // ── Status-based response log ─────────────────────────────
            if (status >= 500) {
                log.error("← {} {} {} {}ms", method, fullPath, status, elapsed);
            } else if (status >= 400) {
                log.warn("← {} {} {} {}ms", method, fullPath, status, elapsed);
            } else if (elapsed > 3000) {
                log.warn("← {} {} {} {}ms ⚠ SLOW", method, fullPath, status, elapsed);
            } else {
                log.info("← {} {} {} {}ms", method, fullPath, status, elapsed);
            }

            // ── Log request/response bodies at DEBUG (with masking) ──
            if (log.isDebugEnabled()) {
                logRequestBody(wrappedRequest);
                logResponseBody(wrappedResponse, status);
            }

            // ── Copy cached body to actual response ───────────────────
            wrappedResponse.copyBodyToResponse();

            // ── Clear MDC — prevent leaking between pooled threads ────
            MDC.remove(MDC_REQUEST_ID);
            MDC.remove(MDC_USER_ID);
            MDC.remove(MDC_ROLE);
            MDC.remove(MDC_CLIENT_IP);
            MDC.remove(MDC_METHOD);
            MDC.remove(MDC_URI);
            MDC.remove(MDC_USER_AGENT);
            MDC.remove(MDC_RESPONSE_STATUS);
            MDC.remove(MDC_CONTENT_LENGTH);
        }
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.startsWith("/css/") ||
               path.startsWith("/js/") ||
               path.startsWith("/images/") ||
               path.startsWith("/fonts/") ||
               path.startsWith("/h2-console") ||
               path.startsWith("/actuator") ||
               path.startsWith("/ws") ||
               path.endsWith(".ico") ||
               path.endsWith(".map") ||
               path.endsWith(".png") ||
               path.endsWith(".jpg") ||
               path.endsWith(".svg");
    }

    // ─── Body logging ─────────────────────────────────────────────────

    private void logRequestBody(ContentCachingRequestWrapper request) {
        byte[] body = request.getContentAsByteArray();
        if (body.length == 0) return;
        if (!isLoggableContentType(request.getContentType())) return;

        String bodyStr = new String(body, 0, Math.min(body.length, MAX_BODY_LOG_SIZE), StandardCharsets.UTF_8);
        bodyStr = SensitiveDataMasker.maskInline(bodyStr);
        if (body.length > MAX_BODY_LOG_SIZE) {
            bodyStr += "…(truncated, total " + body.length + " bytes)";
        }
        log.debug("  Request Body: {}", bodyStr);
    }

    private void logResponseBody(ContentCachingResponseWrapper response, int status) {
        // Only log response bodies for error responses or when explicitly at DEBUG
        if (status < 400 && !log.isTraceEnabled()) return;

        byte[] body = response.getContentAsByteArray();
        if (body.length == 0) return;
        if (!isLoggableContentType(response.getContentType())) return;

        String bodyStr = new String(body, 0, Math.min(body.length, MAX_BODY_LOG_SIZE), StandardCharsets.UTF_8);
        bodyStr = SensitiveDataMasker.maskInline(bodyStr);
        if (body.length > MAX_BODY_LOG_SIZE) {
            bodyStr += "…(truncated, total " + body.length + " bytes)";
        }
        log.debug("  Response Body [{}]: {}", status, bodyStr);
    }

    private boolean isLoggableContentType(String contentType) {
        if (contentType == null) return false;
        String lower = contentType.toLowerCase();
        return LOGGABLE_CONTENT_TYPES.stream().anyMatch(lower::contains);
    }

    // ─── User context ─────────────────────────────────────────────────

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
            return forwarded.split(",")[0].trim();
        }
        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) {
            return realIp;
        }
        return request.getRemoteAddr();
    }

    private String truncate(String value, int maxLen) {
        if (value == null) return "";
        return value.length() > maxLen ? value.substring(0, maxLen) + "…" : value;
    }
}

