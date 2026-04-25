package com.blss.statusservice.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * Event DTO for order status changes.
 * Received via JMS from order-service.
 */
public record OrderStatusChangedEvent(
        UUID id,
        Instant timestamp,
        String status
) {
}
