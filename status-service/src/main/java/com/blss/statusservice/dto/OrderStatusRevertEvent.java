package com.blss.statusservice.dto;

import java.time.Instant;

/**
 * Событие для отката статуса.
 */
public record OrderStatusRevertEvent(
        OrderStatusChangedEvent originalEvent,
        String errorMessage,
        Instant revertTimestamp
) {
}
