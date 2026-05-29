package com.blss.orderservice.service;

import com.blss.orderservice.service.order.OrderProcessService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class StorageService {

    OrderProcessService orderProcessService;

    public void updateYacheyka(UUID itemId, String yacheyka) {
        orderProcessService.markDelivered(itemId, yacheyka);
    }
}
