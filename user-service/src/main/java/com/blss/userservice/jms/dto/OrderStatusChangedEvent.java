package com.blss.userservice.jms.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * Event DTO for order status changes.
 * Received via JMS from order-service.
 *
 * @param id        Order ID
 * @param timestamp Status change timestamp
 * @param status    New order status
 */
public record OrderStatusChangedEvent(
        UUID id,
        Instant timestamp,
        String status
) {
}
