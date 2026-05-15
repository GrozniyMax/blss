package com.blss.orderservice.service;

import com.blss.orderservice.db.order.OrderItemRepo;
import com.blss.orderservice.domain.order.OrderItem;
import com.blss.orderservice.exception.NotFoundException;
import com.blss.orderservice.service.order.OrderStatusUpdater;
import com.blss.orderservice.service.tx.TransactionExecutor;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class StorageService {

    OrderItemRepo orderItemRepo;

    OrderStatusUpdater updater;

    TransactionExecutor transactionExecutor;

    public void updateYacheyka(UUID itemId, String yacheyka) {
        var item = transactionExecutor.inTransaction(() ->
                orderItemRepo.updateYacheyka(itemId, yacheyka)
                        .orElseThrow(() -> new NotFoundException(OrderItem.class, itemId))
        );
        updater.updateStatusIfReady(item.orderId());
    }

    public List<OrderItem> getAllByOrderId(UUID orderId) {
        return orderItemRepo.findAllByOrderId(orderId);
    }
}
