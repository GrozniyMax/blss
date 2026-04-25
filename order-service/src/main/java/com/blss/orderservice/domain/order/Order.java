package com.blss.orderservice.domain.order;

import lombok.Builder;
import lombok.With;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@With
@Builder
@Table("orders")
public record Order(
        @Id
        UUID id,

        Status status,

        /*
         * Имя владельца (username из XML)
         */
        String owner,

        /*
         * Id ПВЗ
         */
        UUID location,

        /*
         * Сумма выдачи
         */
        BigDecimal totalAmount,

        Instant creationDate,

        Instant lastEdited
) { }
