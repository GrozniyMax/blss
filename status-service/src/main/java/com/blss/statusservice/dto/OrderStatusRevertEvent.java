package com.blss.statusservice.dto;

import java.time.Instant;

public record OrderStatusRevertEvent(
        OrderStatusChangedEvent originalEvent,
        String errorMessage,
        Instant revertTimestamp
) {
}
