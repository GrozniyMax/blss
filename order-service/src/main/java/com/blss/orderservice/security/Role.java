package com.blss.orderservice.security;

public enum Role {

    /**
     * Администратор — полный доступ.
     */
    ADMIN,

    /**
     * Менеджер — управление заказами, складом, ПВЗ.
     */
    MANAGER,

    /**
     * Консультант — выдача заказов.
     */
    CONSULTANT,

    /**
     * Склад — отметка доставки.
     */
    WAREHOUSE,

    /**
     * Пользователь — создание заказов.
     */
    USER
}
