package com.blss.statusservice.jms;

import com.blss.statusservice.dto.OrderStatusChangedEvent;
import com.blss.statusservice.service.FailureSimulation;
import com.blss.statusservice.service.OrderStatusHistoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;

/**
 * Слушает изменения статусов из order-service.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OrderStatusConsumer {

    private final OrderStatusHistoryService historyService;
    private final OrderStatusRevertProducer revertProducer;
    private final FailureSimulation failureSimulation;

    @JmsListener(destination = "${jms.queue.order-status-changed}")
    public void onOrderStatusChanged(OrderStatusChangedEvent event) {
        log.info("Received order status change event: orderId={}, status={}, timestamp={}",
                event.id(),
                event.status(),
                event.timestamp());

        if (failureSimulation.isFailInMethod()) {
            throw new RuntimeException("Simulated failure");
        }

        try {
            if (failureSimulation.failInTryCatch()) {
                throw new RuntimeException("Simulated failure it try-catch");
            }
            historyService.saveStatusChange(event.id(), event.status());
            log.info("Order status change event processed: orderId={}", event.id());
        } catch (Exception e) {
            log.error("Failed to process status change: orderId={}, status={}, error={}",
                    event.id(), event.status(), e.getMessage(), e);
            revertProducer.sendRevertRequest(event, e.getMessage());
        }
    }
}
