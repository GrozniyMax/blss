package com.blss.statusservice.dto.output;

import java.time.Instant;

/**
 * DTO for order status history entry.
 */
public record StatusHistoryEntryDto(
        String status,
        Instant changedAt
) {
}
