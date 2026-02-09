package com.blss.blss.exception;

public class NotFoundException extends RuntimeException{

    private final Class<?> entityClass;

    public NotFoundException(Class<?> entityClass, String message) {
        super(message);
        this.entityClass = entityClass;
    }
}
