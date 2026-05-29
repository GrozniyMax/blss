package com.blss.statusservice.dto.input;

import java.util.UUID;

public record StatusTransactionPrepareRequest(
        UUID orderId,
        String status,
        String changedBy
) {
}
