package com.blss.orderservice.jms.dto;

import java.time.Instant;

public record OrderStatusRevertEvent(
        OrderStatusChangedEvent originalEvent,
        String errorMessage,
        Instant revertTimestamp
) {
}
