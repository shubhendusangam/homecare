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
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAccessDeniedHandler implements AccessDeniedHandler {

    private final ObjectMapper objectMapper;

    @Override
    public void handle(HttpServletRequest request,
                       HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException {
        log.warn("SECURITY | event=ACCESS_DENIED | uri={} | method={} | userId={} | ip={} | requestId={}",
                request.getRequestURI(), request.getMethod(),
                MDC.get("userId"), MDC.get("clientIp"), MDC.get("requestId"));

        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);

        ApiResponse<Void> body = ApiResponse.error(
                "You do not have permission to access this resource.",
                ErrorCode.FORBIDDEN.name());

        objectMapper.writeValue(response.getOutputStream(), body);
    }
}

