package com.blss.statusservice.dto;

import java.time.Instant;

/**
 * DTO for order status history entry.
 */
public record StatusHistoryEntryDto(
        String status,
        Instant changedAt
) {
}
