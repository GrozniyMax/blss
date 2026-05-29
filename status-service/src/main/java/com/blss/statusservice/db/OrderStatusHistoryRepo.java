package com.blss.statusservice.db;

import com.blss.statusservice.domain.OrderStatusHistory;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface OrderStatusHistoryRepo extends CrudRepository<OrderStatusHistory, UUID> {

    @Query("SELECT * FROM order_status_history WHERE order_id = :orderId ORDER BY changed_at ASC")
    List<OrderStatusHistory> findByOrderId(UUID orderId);

}
