package com.blss.statusservice.db;

import com.blss.statusservice.domain.OrderStatusHistory;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Repository for order status history.
 */
@Repository
public interface OrderStatusHistoryRepo extends CrudRepository<OrderStatusHistory, UUID> {

    /**
     * Find all status changes for an order, ordered by timestamp.
     */
    @Query("""
            SELECT *
            FROM order_status_history
            WHERE order_id = :orderId
              AND tx_state = 'CONFIRMED'
            ORDER BY changed_at ASC
            """)
    List<OrderStatusHistory> findByOrderId(UUID orderId);

    /**
     * Check if order has any history.
     */
    @Query("SELECT EXISTS(SELECT 1 FROM order_status_history WHERE order_id = :orderId AND tx_state = 'CONFIRMED')")
    boolean existsByOrderId(UUID orderId);

    @Query("SELECT * FROM order_status_history WHERE tx_id = :txId")
    OrderStatusHistory findByTxId(UUID txId);
}
