package com.blss.statusservice.dto;

import java.time.Instant;

public record StatusHistoryEntryDto(
        String status,
        Instant changedAt
) {
}
