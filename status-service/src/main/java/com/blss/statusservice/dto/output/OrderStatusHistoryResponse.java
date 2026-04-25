package com.blss.statusservice.dto.output;

import java.util.List;
import java.util.UUID;

/**
 * DTO for order status history response.
 */
public record OrderStatusHistoryResponse(
        UUID orderId,
        List<StatusHistoryEntryDto> history
) {
}
