package com.homecare.user.security;

import com.homecare.admin.service.BannedUserStore;
import com.homecare.user.enums.Role;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final BannedUserStore bannedUserStore;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String token = extractToken(request);

        if (token != null && jwtUtil.isValid(token)) {
            try {
                UUID userId = jwtUtil.extractUserId(token);

                // Reject banned users immediately
                if (bannedUserStore.isBanned(userId)) {
                    log.debug("Rejecting request from banned user {}", userId);
                    response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                    response.setContentType("application/json");
                    response.getWriter().write("{\"success\":false,\"message\":\"Account has been suspended\",\"errorCode\":\"FORBIDDEN\"}");
                    return;
                }

                String roleStr = jwtUtil.extractRole(token);
                String email = jwtUtil.extractEmail(token);
                Role role = Role.valueOf(roleStr);

                UserPrincipal principal = new UserPrincipal(userId, email, role);

                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                SecurityContextHolder.getContext().setAuthentication(authentication);

                // Push user context into MDC so all downstream logs include it
                MDC.put("userId", userId.toString());
                MDC.put("role", roleStr);
            } catch (Exception e) {
                log.error("Could not set user authentication from JWT", e);
            }
        }

        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove("userId");
            MDC.remove("role");
        }
    }

    private String extractToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (StringUtils.hasText(header) && header.startsWith("Bearer ")) {
            return header.substring(7);
        }
        return null;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        // /api/v1/auth/logout requires a valid token — don't skip it
        if (path.equals("/api/v1/auth/logout")) {
            return false;
        }
        return path.startsWith("/api/v1/auth/") ||
               path.startsWith("/h2-console") ||
               path.startsWith("/css/") ||
               path.startsWith("/js/") ||
               path.equals("/") ||
               path.equals("/index.html") ||
               path.startsWith("/ws");
    }
}

