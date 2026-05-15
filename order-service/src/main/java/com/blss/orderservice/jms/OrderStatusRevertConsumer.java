package com.blss.orderservice.jms;

import com.blss.orderservice.jms.dto.OrderStatusRevertEvent;
import com.blss.orderservice.service.order.OrderStatusUpdater;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;

/**
 * Получает запросы на откат статуса.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OrderStatusRevertConsumer {

    private final OrderStatusUpdater statusUpdater;

    @JmsListener(destination = "${jms.queue.order-status-revert}")
    public void onRevertRequest(OrderStatusRevertEvent revert) {
        var originalEvent = revert.originalEvent();
        
        log.warn("Received revert request: orderId={}, failedStatus={}, error={}",
                originalEvent.id(),
                originalEvent.status(),
                revert.errorMessage());

        statusUpdater.revertStatus(originalEvent.id(), originalEvent.status());
    }
}
