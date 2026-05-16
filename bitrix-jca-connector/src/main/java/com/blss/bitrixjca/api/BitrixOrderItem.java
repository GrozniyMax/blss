package com.blss.bitrixjca.api;

import java.math.BigDecimal;
import java.util.UUID;

public record BitrixOrderItem(
        UUID itemId,
        UUID productId,
        String productName,
        BigDecimal price,
        String yacheyka
) {
}
