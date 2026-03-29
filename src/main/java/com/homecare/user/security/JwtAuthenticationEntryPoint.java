package com.homecare.user.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.homecare.core.dto.ApiResponse;
import com.homecare.core.enums.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        log.warn("SECURITY | event=UNAUTHENTICATED | uri={} | method={} | ip={} | requestId={} | reason={}",
                request.getRequestURI(), request.getMethod(),
                MDC.get("clientIp"), MDC.get("requestId"),
                authException.getMessage());

        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);

        ApiResponse<Void> body = ApiResponse.error(
                "Authentication required. Please provide a valid token.",
                ErrorCode.UNAUTHORIZED.name());

        objectMapper.writeValue(response.getOutputStream(), body);
    }
}

