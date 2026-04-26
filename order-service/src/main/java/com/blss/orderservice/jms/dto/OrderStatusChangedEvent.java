package com.blss.orderservice.jms.dto;

import com.blss.orderservice.domain.order.Status;

import java.time.Instant;
import java.util.UUID;

/**
 * Event DTO for order status changes.
 * Sent via JMS to notify other services about status updates.
 *
 * @param id        Order ID
 * @param timestamp Status change timestamp
 * @param status    New order status
 */
public record OrderStatusChangedEvent(
        UUID id,
        Instant timestamp,
        Status status
) {
}
