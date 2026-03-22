package com.blss.blss.exception;

public class NotFoundException extends RuntimeException {
    private final Class<?> entityClass;
    private final Object id;

    public NotFoundException(Class<?> entityClass) {
        this(entityClass, null);
    }

    public NotFoundException(Class<?> entityClass, Object id) {
        this.entityClass = entityClass;
        this.id = id;
    }

    @Override
    public String getMessage() {
        return String.format("Entity %s  with %s not found", entityClass.getSimpleName(), id);
    }

    public String getId() {
        return id == null ? null : id.toString();
    }

}
