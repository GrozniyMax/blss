package com.blss.statusservice.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Custom access denied handler that returns JSON response on 403 Forbidden.
 */
@Component
@RequiredArgsConstructor
public class CustomAccessDeniedHandler implements AccessDeniedHandler {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(CustomAccessDeniedHandler.class);

    private final ObjectMapper objectMapper;

    @Override
    public void handle(
            HttpServletRequest request,
            HttpServletResponse response,
            AccessDeniedException accessDeniedException) {

        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        try {
            Map<String, Object> errorResponse = Map.of(
                "timestamp", LocalDateTime.now().toString(),
                "status", 403,
                "error", "Forbidden",
                "message", "Access denied: " + accessDeniedException.getMessage(),
                "path", request.getRequestURI()
            );
            response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
        } catch (Exception e) {
            log.error("Error writing access denied response", e);
        }
    }
}
