package com.blss.statusservice.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Custom authentication entry point that returns JSON response on 401 Unauthorized.
 */
@Component
@RequiredArgsConstructor
public class CustomAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(CustomAuthenticationEntryPoint.class);

    private final RequestMappingHandlerMapping handlerMapping;
    private final ObjectMapper objectMapper;

    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException authException) {

        Object handler;
        try {
            handler = handlerMapping.getHandler(request);
        } catch (Exception e) {
            log.debug("No handler for {}", request.getRequestURI(), e);
            handler = null;
        }

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        try {
            if (handler == null) {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                Map<String, Object> errorResponse = Map.of(
                    "timestamp", LocalDateTime.now().toString(),
                    "status", 404,
                    "error", "Not Found",
                    "message", "Endpoint not found: " + request.getMethod() + " " + request.getRequestURI(),
                    "path", request.getRequestURI()
                );
                response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
            } else {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                Map<String, Object> errorResponse = Map.of(
                    "timestamp", LocalDateTime.now().toString(),
                    "status", 401,
                    "error", "Unauthorized",
                    "message", "Authentication required",
                    "path", request.getRequestURI()
                );
                response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
            }
        } catch (Exception e) {
            log.error("Error writing authentication error response", e);
        }
    }
}
