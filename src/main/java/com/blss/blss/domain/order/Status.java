package com.blss.blss.domain.order;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.jspecify.annotations.Nullable;

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

    public Status next() {
        return switch (this) {
            case CREATED -> PROCESSING;
            case PROCESSING-> IN_DELIVERY;
            case IN_DELIVERY-> READY_FOR_PICKUP;
            case READY_FOR_PICKUP-> DONE;
            default -> throw new IllegalStateException("Unexpected value: " + this);
        }
    }

}
