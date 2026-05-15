package com.blss.orderservice.domain.order;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum Status {
    /**
     * Заказ создан
     */
    CREATED,
    /**
     * В обработке
     */
    PROCESSING,
    /**
     * Доставляется
     */
    IN_DELIVERY,
    /**
     * Готов к выдаче
     */
    READY_FOR_PICKUP,

    /**
     * Отменен или возвращен
     */
    CANCELED,

    /**
     * Клиент забрал заказ
     */
    DONE;

    @JsonValue
    public String toJson() {
        return name();
    }

    @JsonCreator
    public static Status fromJson(String value) {
        return valueOf(value.toUpperCase());
    }
}
