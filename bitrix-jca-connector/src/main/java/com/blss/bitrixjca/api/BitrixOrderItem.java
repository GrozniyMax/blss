package com.blss.bitrixjca.api;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Represents an order item for Bitrix24 document generation.
 *
 * @param itemId     Order item ID
 * @param productId  Product ID
 * @param productName Product name
 * @param price      Product price
 * @param yacheyka   Cell/location identifier (optional)
 */
public record BitrixOrderItem(
        UUID itemId,
        UUID productId,
        String productName,
        BigDecimal price,
        String yacheyka
) {
}
