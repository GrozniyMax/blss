package com.blss.blss.exception;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AlreadyExistsException extends RuntimeException {

    Class<?> entityClass;

    @Override
    public String getMessage() {
        return String.format("Entity %s already exists", entityClass.getSimpleName());
    }
}
