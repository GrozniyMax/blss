package com.blss.orderservice.jms.dto;

import java.time.Instant;

/**
 * Событие отката от status-service.
 */
public record OrderStatusRevertEvent(
        OrderStatusChangedEvent originalEvent,
        String errorMessage,
        Instant revertTimestamp
) {
}
