package com.homecare.core.config;

import com.homecare.user.enums.Role;
import com.homecare.user.security.JwtUtil;
import com.homecare.user.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Intercepts STOMP CONNECT frames to authenticate the WebSocket connection
 * using JWT from the Authorization header or a "token" native header.
 * Sets userId + role in session attributes for downstream message handlers.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class WebSocketAuthInterceptor implements ChannelInterceptor {

    private final JwtUtil jwtUtil;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor =
                MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
            String token = extractToken(accessor);

            if (token != null && jwtUtil.isValid(token)) {
                UUID userId = jwtUtil.extractUserId(token);
                String roleStr = jwtUtil.extractRole(token);
                String email = jwtUtil.extractEmail(token);
                Role role = Role.valueOf(roleStr);

                UserPrincipal principal = new UserPrincipal(userId, email, role);
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());

                accessor.setUser(authentication);

                // Store in session attributes for @MessageMapping handlers
                if (accessor.getSessionAttributes() != null) {
                    accessor.getSessionAttributes().put("userId", userId);
                    accessor.getSessionAttributes().put("role", roleStr);
                    accessor.getSessionAttributes().put("email", email);
                }

                log.debug("WebSocket CONNECT authenticated: userId={}, role={}", userId, roleStr);
            } else {
                log.warn("WebSocket CONNECT rejected: invalid or missing JWT");
                throw new IllegalArgumentException("Invalid or missing JWT token");
            }
        }

        return message;
    }

    private String extractToken(StompHeaderAccessor accessor) {
        // Try Authorization header first: "Bearer <token>"
        String authHeader = accessor.getFirstNativeHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }

        // Fallback: "token" header (browsers can't set WS headers, so STOMP native header)
        String tokenHeader = accessor.getFirstNativeHeader("token");
        if (tokenHeader != null && !tokenHeader.isBlank()) {
            return tokenHeader;
        }

        return null;
    }
}

