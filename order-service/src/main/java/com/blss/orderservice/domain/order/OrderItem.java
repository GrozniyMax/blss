package com.blss.orderservice.domain.order;


import lombok.With;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.util.UUID;

@With
@Table("order_item")
public record OrderItem(

        @Id
        UUID id,
        UUID orderId,
        UUID productId,
        String yacheyka
) {
}
