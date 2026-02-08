package com.blss.blss.domain.order;

import lombok.Builder;
import lombok.With;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@With
@Builder
@Table("order")
public record Order(
        @Id
        UUID id,

        Status status,

        UUID owner,

        /*
         * Id ПВЗ
         */
        UUID localtion,

        /*
         * Сумма выдачи
         */
        BigDecimal totalAmount,

        Instant creationDate,

        Instant lastEdited
) { }
