package com.blss.blss.db.order;

import com.blss.blss.domain.order.Order;
import com.blss.blss.domain.order.Status;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;

import java.util.Optional;
import java.util.UUID;

public interface OrderRepo extends CrudRepository<Order, UUID> {

    @Query("""
            UPDATE orders
            SET
                status = :#{#order.status},
                owner = :#{#order.owner},
                localtion = :#{#order.localtion},
                total_amount = :#{#order.totalAmount},
                creation_date = :#{#order.creationDate},
                last_edited = NOW()
            WHERE id = :#{#order.id} RETURNING *
            """)
    Order update(Order order);

    @Query("""
            INSERT INTO orders (
                status,
                owner,
                localtion,
                total_amount,
                creation_date,
                last_edited
            ) VALUES (
                :#{#order.status},
                :#{#order.owner},
                :#{#order.localtion},
                :#{#order.totalAmount},
                NOW(),
                NULL
            )
            RETURNING *
            """)
    Order create(Order order);

    @Query("UPDATE orders SET status = :status, last_edited = NOW() WHERE id = :id RETURNING *")
    Optional<Order> updateStatus(UUID id, Status status);
}
