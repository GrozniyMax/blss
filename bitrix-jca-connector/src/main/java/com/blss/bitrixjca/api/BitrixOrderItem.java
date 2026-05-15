package com.blss.bitrixjca.api;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Позиция заказа для документа Bitrix24.
 */
public record BitrixOrderItem(
        UUID itemId,
        UUID productId,
        String productName,
        BigDecimal price,
        String yacheyka
) {
}
