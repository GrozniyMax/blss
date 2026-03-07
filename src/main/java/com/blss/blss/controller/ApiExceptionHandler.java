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
import java.util.UUID;

@Slf4j
@RestControllerAdvice
public class ApiExceptionHandler {

    @ResponseStatus(HttpStatus.NOT_FOUND)
    @ExceptionHandler(NotFoundException.class)
    public ErrorResponseDto handleNotFound(NotFoundException e, HttpServletRequest request) {
        return build(e, request.getRequestURI());
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler({AlreadyExistsException.class,
            InvalidOrderException.class,
            IllegalArgumentException.class,
            UpdateException.class})
    public ErrorResponseDto handleBadRequestException(Exception e, HttpServletRequest request) {
        return build(e, request.getRequestURI());
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ErrorResponseDto handleValidation(MethodArgumentNotValidException e, HttpServletRequest request) {
        String message = e.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(this::formatFieldError)
                .distinct()
                .reduce((left, right) -> left + "; " + right)
                .orElse("Validation failed");

        return new ErrorResponseDto(
                message,
                Instant.now().toString(),
                request.getRequestURI(),
                UUID.randomUUID().toString()
        );
    }

    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    @ExceptionHandler(Exception.class)
    public ErrorResponseDto handleError(Exception e, HttpServletRequest request) {
        log.error("Не смогли обработать исключение", e);
        return new ErrorResponseDto(
                "Произошла непредвиденная ошибка",
                Instant.now().toString(),
                request.getRequestURI(),
                UUID.randomUUID().toString()
        );
    }

    private ErrorResponseDto build(Exception e, String uri) {
        return new ErrorResponseDto(
                e.getMessage(),
                Instant.now().toString(),
                uri,
                UUID.randomUUID().toString()
        );
    }

    private String formatFieldError(FieldError error) {
        String details = error.getDefaultMessage() == null ? "invalid value" : error.getDefaultMessage();
        return error.getField() + ": " + details;
    }
}
