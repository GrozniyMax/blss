package com.blss.blss.db.order;

import com.blss.blss.domain.order.OrderItem;

import java.util.List;
import java.util.UUID;

public interface OrderItemExtension {

    /**
     * Создание нескольких заказов. Возвращает список id созданных заказов.<br>
     * <strong>У всех элементов должен быть один orderId</strong>
     * @param items
     * @return
     */
    List<UUID> create(List<OrderItem> items);
}
