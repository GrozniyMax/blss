package com.blss.statusservice.domain;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * Order status history entity.
 * Stores all status changes for an order.
 */
@Table("order_status_history")
public record OrderStatusHistory(
        @Id
        UUID id,

        UUID orderId,

        String status,

        Instant changedAt,

        UUID txId,

        String txState
) {
    /**
     * Create a new entity without an ID (for new records).
     * Spring Data JDBC will generate the ID on save.
     */
    public OrderStatusHistory(UUID orderId, String status, Instant changedAt) {
        this(null, orderId, status, changedAt, null, "CONFIRMED");
    }

    public OrderStatusHistory(UUID orderId, String status, Instant changedAt, UUID txId, String txState) {
        this(null, orderId, status, changedAt, txId, txState);
    }
}
