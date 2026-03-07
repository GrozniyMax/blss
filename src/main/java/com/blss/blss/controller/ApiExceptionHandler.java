package com.blss.blss.controller;

import com.blss.blss.dto.output.ErrorResponseDto;
import com.blss.blss.exception.AlreadyExistsException;
import com.blss.blss.exception.InvalidOrderException;
import com.blss.blss.exception.NotFoundException;
import com.blss.blss.exception.UpdateException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;

@Slf4j
@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ErrorResponseDto> handleNotFound(NotFoundException e, HttpServletRequest request) {
        return handleException(e, HttpStatus.NOT_FOUND, request);
    }

    @ExceptionHandler({
            AlreadyExistsException.class,
            InvalidOrderException.class,
            UpdateException.class,
            IllegalArgumentException.class
    })
    public ResponseEntity<ErrorResponseDto> handleBadRequest(Exception e, HttpServletRequest request) {
        return handleException(e, HttpStatus.BAD_REQUEST, request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponseDto> handleValidation(MethodArgumentNotValidException e, HttpServletRequest request) {
        String message = e.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(this::formatFieldError)
                .distinct()
                .reduce((left, right) -> left + "; " + right)
                .orElse("Validation failed");
        return buildResponse(message, HttpStatus.BAD_REQUEST, request, null);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponseDto> handleError(Exception e, HttpServletRequest request) {
        return handleException(e, HttpStatus.INTERNAL_SERVER_ERROR, request);
    }

    private ResponseEntity<ErrorResponseDto> handleException(Exception e, HttpStatus status, HttpServletRequest request) {
        log.error("Error", e);
        String message = e.getMessage() == null || e.getMessage().isBlank()
                ? "Unexpected server error"
                : e.getMessage();
        return buildResponse(message, status, request, e);
    }

    private ResponseEntity<ErrorResponseDto> buildResponse(String message, HttpStatus status, HttpServletRequest request, Exception e) {
        ErrorResponseDto body = new ErrorResponseDto(
                message,
                Instant.now().toString(),
                request.getRequestURI(),
                extractEntityId(e)
        );
        return ResponseEntity.status(status).body(body);
    }

    private String formatFieldError(FieldError error) {
        String details = error.getDefaultMessage() == null ? "invalid value" : error.getDefaultMessage();
        return error.getField() + ": " + details;
    }

    private String extractEntityId(Exception e) {
        if (e instanceof NotFoundException notFoundException) {
            return notFoundException.getId();
        }
        if (e instanceof UpdateException updateException) {
            return updateException.getId();
        }
        return null;
    }
}
