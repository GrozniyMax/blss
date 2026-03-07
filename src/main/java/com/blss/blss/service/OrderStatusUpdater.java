package com.blss.blss.service;

import com.blss.blss.db.order.OrderItemRepo;
import com.blss.blss.domain.order.Status;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class OrderStatusUpdater {

    OrderItemRepo orderItemRepo;

    OrderService service;

    @Async
    public void updateStatusIfReady(UUID orderId) {
        if (orderItemRepo.countItemsWithoutYacheyka(orderId) == 0) {
            service.updateStatus(orderId, Status.READY_FOR_PICKUP);
        }
    }
}
