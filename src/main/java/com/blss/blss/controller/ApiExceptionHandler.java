package com.blss.blss.controller;

import com.blss.blss.exception.AlreadyExistsException;
import com.blss.blss.exception.InvalidOrderException;
import com.blss.blss.exception.NotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler({
            AlreadyExistsException.class,
            InvalidOrderException.class,
            NotFoundException.class,
            IllegalArgumentException.class
    })
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseEntity<?> handleBadRequest(Exception e) {
        return handleGenericException(e);
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ResponseEntity<?> handleError(Exception e) {
        return handleGenericException(e);
    }

    private ResponseEntity<?> handleGenericException(Exception e) {
        log.error("Error", e);
        return ResponseEntity.badRequest().body(e.getMessage());
    }
}