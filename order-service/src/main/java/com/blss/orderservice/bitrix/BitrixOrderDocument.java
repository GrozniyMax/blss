package com.blss.orderservice.bitrix;

import java.math.BigDecimal;
import java.util.UUID;

public record BitrixOrderDocument(
        UUID orderId,
        String title,
        String body,
        BigDecimal totalAmount
) {
}
