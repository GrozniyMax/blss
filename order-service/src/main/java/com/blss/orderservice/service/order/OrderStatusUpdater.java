package com.blss.orderservice.service.order;

import com.blss.orderservice.domain.order.Status;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class OrderStatusUpdater {

    OrderProcessService orderProcessService;

    @Async
    public void updateStatusIfReady(UUID orderId) {
        orderProcessService.checkDeliveryReadiness(orderId);
    }

    public void next(UUID orderId) {
        orderProcessService.advanceStatus(orderId);
    }

    public void revertStatus(UUID orderId, Status revertFrom) {
        orderProcessService.revertStatus(orderId, revertFrom.name());
    }

    public void cancel(UUID orderId) {
        orderProcessService.cancel(orderId);
    }
}
