package com.blss.orderservice.domain;

import lombok.Builder;
import lombok.With;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@With
@Builder
@Table("sold_products")
public record SoldProduct(
        @Id
        UUID id,

        UUID productId,

        BigDecimal price,

        Instant soldAt
) {
}
