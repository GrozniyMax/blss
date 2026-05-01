package com.blss.orderservice.bitrix;

import java.math.BigDecimal;
import java.util.UUID;

public record BitrixOrderDocumentItem(
        UUID itemId,
        UUID productId,
        String productName,
        BigDecimal price,
        String yacheyka
) {
}
