package com.blss.orderservice.domain.order;

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

}
