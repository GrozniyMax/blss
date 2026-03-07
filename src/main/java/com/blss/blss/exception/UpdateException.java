package com.blss.blss.exception;

public class UpdateException extends RuntimeException {
    private final Object id;

    public UpdateException(Object id, String message) {
        super(message);
        this.id = id;
    }

    public String getId() {
        return id == null ? null : id.toString();
    }
}
