package com.blss.blss.service;

import com.blss.blss.db.order.OrderItemRepo;
import com.blss.blss.domain.order.OrderItem;
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

    public void updateYacheyka(UUID itemId, String yacheyka) {
        var item = orderItemRepo.updateYacheyka(itemId, yacheyka);
        updater.updateStatusIfReady(item.orderId());
    }

    public List<OrderItem> getAllByOrderId(UUID orderId) {
        return orderItemRepo.findAllByOrderId(orderId);
    }
}
