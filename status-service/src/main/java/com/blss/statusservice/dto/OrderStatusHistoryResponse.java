package com.blss.statusservice.dto;

import java.util.List;
import java.util.UUID;

public record OrderStatusHistoryResponse(
        UUID orderId,
        List<StatusHistoryEntryDto> history
) {
}
