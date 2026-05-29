package com.blss.orderservice.client.dto;

import java.util.UUID;

public record StatusTransactionPrepareRequest(
        UUID orderId,
        String status,
        String changedBy
) {
}
