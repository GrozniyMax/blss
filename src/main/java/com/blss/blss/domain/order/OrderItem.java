package com.blss.blss.domain.order;


import lombok.With;

import java.util.UUID;

/**
 * Описание элемента заказа
 * @param orderId
 * @param storeItem
 * @param yacheyka
 */
@With
record OrderItem(

    UUID orderId,
    UUID storeItem,
    /**
     * Адрес ячейки. Может быть null
     */
    String yacheyka
) { }
