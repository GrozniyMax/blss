package com.blss.orderservice.bitrix;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record BitrixOrderDocument(
        UUID orderId,
        String title,
        String body,
        BigDecimal totalAmount,
        String owner,
        String status,
        Instant createdAt,
        String deliveryPointName,
        String deliveryPointAddress,
        List<BitrixOrderDocumentItem> items
) {
}
