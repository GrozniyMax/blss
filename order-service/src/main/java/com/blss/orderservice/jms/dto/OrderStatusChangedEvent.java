package com.blss.orderservice.jms.dto;

import com.blss.orderservice.domain.order.Status;

import java.time.Instant;
import java.util.UUID;

public record OrderStatusChangedEvent(
        UUID id,
        Instant timestamp,
        Status status
) {
}
