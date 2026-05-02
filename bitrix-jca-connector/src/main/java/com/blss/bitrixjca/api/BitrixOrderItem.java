package com.blss.bitrixjca.api;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Represents an order item for Bitrix24 document generation.
 *
 * @param itemId     Order item ID
 * @param productId  Product ID
 * @param productName Product name
 * @param price      Product price per unit
 * @param quantity   Product quantity (default: 1)
 * @param yacheyka   Cell/location identifier (optional)
 */
public record BitrixOrderItem(
        UUID itemId,
        UUID productId,
        String productName,
        BigDecimal price,
        Integer quantity,
        String yacheyka
) {
    /**
     * Compact constructor - sets default quantity to 1 if null.
     */
    public BitrixOrderItem {
        if (quantity == null) {
            quantity = 1;
        }
    }
}
