package com.blss.statusservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Global exception handler for status-service.
 */
@RestControllerAdvice
public class ApiExceptionHandler {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(ApiExceptionHandler.class);

    private final ObjectMapper objectMapper = new ObjectMapper();

    @ResponseStatus(HttpStatus.FORBIDDEN)
    @ExceptionHandler(AuthorizationDeniedException.class)
    public Map<String, Object> handleAuthorizationDeniedException(AuthorizationDeniedException e, HttpServletRequest request) {
        return build(e, request.getRequestURI(), 403);
    }

    @ResponseStatus(HttpStatus.NOT_FOUND)
    @ExceptionHandler(NotFoundException.class)
    public Map<String, Object> handleNotFound(NotFoundException e, HttpServletRequest request) {
        return build(e, request.getRequestURI(), 404);
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler({
            IllegalArgumentException.class,
            HttpMessageNotReadableException.class
    })
    public Map<String, Object> handleBadRequestException(Exception e, HttpServletRequest request) {
        return build(e, request.getRequestURI(), 400);
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Map<String, Object> handleValidation(MethodArgumentNotValidException e, HttpServletRequest request) {
        String message = e.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(this::formatFieldError)
                .distinct()
                .reduce((left, right) -> left + "; " + right)
                .orElse("Validation failed");

        return Map.of(
            "error", "Validation Error",
            "message", message,
            "timestamp", Instant.now().toString(),
            "path", request.getRequestURI(),
            "status", 400
        );
    }

    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    @ExceptionHandler(Exception.class)
    public Map<String, Object> handleError(Exception e, HttpServletRequest request) {
        log.error("Unexpected error", e);
        return Map.of(
            "error", "Internal Server Error",
            "message", "Произошла непредвиденная ошибка",
            "timestamp", Instant.now().toString(),
            "path", request.getRequestURI(),
            "status", 500
        );
    }

    private Map<String, Object> build(Exception e, String uri, int status) {
        return Map.of(
            "error", e.getClass().getSimpleName(),
            "message", e.getMessage(),
            "timestamp", Instant.now().toString(),
            "path", uri,
            "status", status
        );
    }

    private String formatFieldError(FieldError error) {
        String details = error.getDefaultMessage() == null ? "invalid value" : error.getDefaultMessage();
        return error.getField() + ": " + details;
    }

    /**
     * Exception for resource not found.
     */
    public static class NotFoundException extends RuntimeException {
        public NotFoundException(String message) {
            super(message);
        }

        public NotFoundException(Class<?> entityClass, Object id) {
            super(String.format("Entity %s with %s not found", entityClass.getSimpleName(), id));
        }
    }
}
