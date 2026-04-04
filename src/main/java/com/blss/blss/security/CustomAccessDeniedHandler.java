package com.blss.blss.security;

import com.blss.blss.dto.output.ErrorResponseDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Custom access denied handler that returns JSON response on 403 Forbidden.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CustomAccessDeniedHandler implements AccessDeniedHandler {

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
            ErrorResponseDto dto = new ErrorResponseDto(
                    "Access Denied",
                    LocalDateTime.now().toString(),
                    request.getRequestURI(),
                    "403"
            );

            response.getWriter().write(objectMapper.writeValueAsString(dto));
        } catch (Exception e) {
            log.error("Error writing access denied response", e);
        }
    }
}
