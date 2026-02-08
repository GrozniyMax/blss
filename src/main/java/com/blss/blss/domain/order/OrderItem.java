package com.blss.blss.domain.order;


import lombok.With;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.util.UUID;

/**
 * Описание элемента заказа
 *
 * @param orderId   идентификатор заказа (ссылается на {@link Order})
 * @param productId идентификатор продукта (ссылается на {@link com.blss.blss.domain.Product}
 * @param yacheyka  описание ячейки
 */
@With
@Table("order_item")
public record OrderItem(

        @Id
        UUID id,
        UUID orderId,
        UUID productId,
        /**
         * Адрес ячейки. Может быть null
         */
        String yacheyka
) {
}
