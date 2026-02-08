package com.blss.blss.db.order;

import com.blss.blss.domain.order.OrderItem;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;

import java.util.UUID;

public interface OrderItemRepo extends CrudRepository<OrderItem, UUID>, OrderItemExtension {

    @Query("""
            INSERT INTO order_item (
                order_id,
                product_id,
                yacheyka
            ) VALUES (
                :#{#orderItem.orderId},
                :#{#orderItem.productId},
                :#{#orderItem.yacheyka}
            )
            RETURNING *
            """)
    OrderItem create(OrderItem orderItem);

    @Query("UPDATE order_item SET yacheyka = :yacheyka WHERE id = :id RETURNING *")
    OrderItem updateYacheyka(UUID id, String yacheyka);

}
