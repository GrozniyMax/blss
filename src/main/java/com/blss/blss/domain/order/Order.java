package com.blss.blss.domain.order;

import lombok.With;
import org.springframework.data.annotation.Id;

import java.time.Instant;
import java.util.UUID;

@With
record Order(
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
        Integer totalAmount,

        Instant creationDate,

        Instant lastEdited
) { }
