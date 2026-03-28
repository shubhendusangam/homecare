package com.homecare.user.security;

import com.homecare.admin.service.BannedUserStore;
import com.homecare.user.enums.Role;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("JwtAuthenticationFilter")
class JwtAuthenticationFilterTest {

    @Mock private JwtUtil jwtUtil;
    @Mock private BannedUserStore bannedUserStore;
    @Mock private HttpServletRequest request;
    @Mock private HttpServletResponse response;
    @Mock private FilterChain filterChain;

    @InjectMocks private JwtAuthenticationFilter filter;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
        MDC.clear();
    }

    @Nested
    @DisplayName("valid JWT")
    class ValidJwt {

        @Test
        @DisplayName("sets SecurityContext and MDC values")
        void setsSecurityContextAndMdc() throws Exception {
            UUID userId = UUID.randomUUID();
            when(request.getHeader("Authorization")).thenReturn("Bearer valid-token");
            when(jwtUtil.isValid("valid-token")).thenReturn(true);
            when(jwtUtil.extractUserId("valid-token")).thenReturn(userId);
            when(jwtUtil.extractRole("valid-token")).thenReturn("CUSTOMER");
            when(jwtUtil.extractEmail("valid-token")).thenReturn("test@example.com");
            when(bannedUserStore.isBanned(userId)).thenReturn(false);

            // Track MDC values during filter chain execution
            doAnswer(inv -> {
                // Inside filterChain.doFilter, MDC should have userId
                assertEquals(userId.toString(), MDC.get("userId"));
                assertEquals("CUSTOMER", MDC.get("role"));
                return null;
            }).when(filterChain).doFilter(request, response);

            filter.doFilterInternal(request, response, filterChain);

            verify(filterChain).doFilter(request, response);
            assertNotNull(SecurityContextHolder.getContext().getAuthentication());
        }

        @Test
        @DisplayName("BUG FIX 1: MDC is cleared after filter chain completes")
        void mdcClearedAfterFilter() throws Exception {
            UUID userId = UUID.randomUUID();
            when(request.getHeader("Authorization")).thenReturn("Bearer valid-token");
            when(jwtUtil.isValid("valid-token")).thenReturn(true);
            when(jwtUtil.extractUserId("valid-token")).thenReturn(userId);
            when(jwtUtil.extractRole("valid-token")).thenReturn("CUSTOMER");
            when(jwtUtil.extractEmail("valid-token")).thenReturn("test@example.com");
            when(bannedUserStore.isBanned(userId)).thenReturn(false);

            filter.doFilterInternal(request, response, filterChain);

            // After filter completes, MDC should be cleared
            assertNull(MDC.get("userId"), "MDC userId should be cleared after filter");
            assertNull(MDC.get("role"), "MDC role should be cleared after filter");
        }

        @Test
        @DisplayName("BUG FIX 1: MDC is cleared even when filter chain throws")
        void mdcClearedOnException() throws Exception {
            UUID userId = UUID.randomUUID();
            when(request.getHeader("Authorization")).thenReturn("Bearer valid-token");
            when(jwtUtil.isValid("valid-token")).thenReturn(true);
            when(jwtUtil.extractUserId("valid-token")).thenReturn(userId);
            when(jwtUtil.extractRole("valid-token")).thenReturn("CUSTOMER");
            when(jwtUtil.extractEmail("valid-token")).thenReturn("test@example.com");
            when(bannedUserStore.isBanned(userId)).thenReturn(false);

            doThrow(new RuntimeException("Simulated error"))
                    .when(filterChain).doFilter(request, response);

            assertThrows(RuntimeException.class,
                    () -> filter.doFilterInternal(request, response, filterChain));

            // MDC should still be cleared
            assertNull(MDC.get("userId"), "MDC userId should be cleared even on exception");
            assertNull(MDC.get("role"), "MDC role should be cleared even on exception");
        }
    }

    @Nested
    @DisplayName("banned user")
    class BannedUser {

        @Test
        @DisplayName("returns 403 and does not proceed")
        void returns403() throws Exception {
            UUID userId = UUID.randomUUID();
            when(request.getHeader("Authorization")).thenReturn("Bearer valid-token");
            when(jwtUtil.isValid("valid-token")).thenReturn(true);
            when(jwtUtil.extractUserId("valid-token")).thenReturn(userId);
            when(bannedUserStore.isBanned(userId)).thenReturn(true);

            StringWriter sw = new StringWriter();
            when(response.getWriter()).thenReturn(new PrintWriter(sw));

            filter.doFilterInternal(request, response, filterChain);

            verify(response).setStatus(HttpServletResponse.SC_FORBIDDEN);
            verify(filterChain, never()).doFilter(any(), any());
            assertTrue(sw.toString().contains("Account has been suspended"));
        }
    }

    @Nested
    @DisplayName("missing/invalid token")
    class MissingToken {

        @Test
        @DisplayName("no Authorization header → passes through unauthenticated")
        void noHeader() throws Exception {
            when(request.getHeader("Authorization")).thenReturn(null);

            filter.doFilterInternal(request, response, filterChain);

            verify(filterChain).doFilter(request, response);
            assertNull(SecurityContextHolder.getContext().getAuthentication());
        }

        @Test
        @DisplayName("invalid token → passes through unauthenticated")
        void invalidToken() throws Exception {
            when(request.getHeader("Authorization")).thenReturn("Bearer invalid-token");
            when(jwtUtil.isValid("invalid-token")).thenReturn(false);

            filter.doFilterInternal(request, response, filterChain);

            verify(filterChain).doFilter(request, response);
            assertNull(SecurityContextHolder.getContext().getAuthentication());
        }
    }

    @Nested
    @DisplayName("shouldNotFilter")
    class ShouldNotFilter {

        @Test
        @DisplayName("auth endpoints (except logout) are skipped")
        void authEndpointsSkipped() {
            when(request.getRequestURI()).thenReturn("/api/v1/auth/login");
            assertTrue(filter.shouldNotFilter(request));

            when(request.getRequestURI()).thenReturn("/api/v1/auth/register/customer");
            assertTrue(filter.shouldNotFilter(request));
        }

        @Test
        @DisplayName("logout endpoint is NOT skipped")
        void logoutNotSkipped() {
            when(request.getRequestURI()).thenReturn("/api/v1/auth/logout");
            assertFalse(filter.shouldNotFilter(request));
        }

        @Test
        @DisplayName("static resources are skipped")
        void staticResourcesSkipped() {
            when(request.getRequestURI()).thenReturn("/css/app.css");
            assertTrue(filter.shouldNotFilter(request));

            when(request.getRequestURI()).thenReturn("/js/app.js");
            assertTrue(filter.shouldNotFilter(request));
        }

        @Test
        @DisplayName("API endpoints are NOT skipped")
        void apiEndpointsNotSkipped() {
            when(request.getRequestURI()).thenReturn("/api/v1/bookings");
            assertFalse(filter.shouldNotFilter(request));
        }
    }
}

