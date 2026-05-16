package com.blss.statusservice.dto;

import java.time.Instant;
import java.util.UUID;

public record OrderStatusChangedEvent(
        UUID id,
        Instant timestamp,
        String status
) {
}
