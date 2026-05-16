package com.blss.statusservice.domain;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;
import java.util.UUID;

@Table("order_status_history")
public record OrderStatusHistory(
        @Id
        UUID id,

        UUID orderId,

        String status,

        Instant changedAt
) {
}
