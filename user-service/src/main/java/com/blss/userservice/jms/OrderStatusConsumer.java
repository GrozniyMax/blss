package com.blss.userservice.jms;

import com.blss.userservice.jms.dto.OrderStatusChangedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;

/**
 * JMS consumer for receiving order status change events from order-service.
 */
@Component
@Slf4j
public class OrderStatusConsumer {

    /**
     * Listens for order status change events from order-service.
     *
     * @param event Order status changed event
     */
    @JmsListener(destination = "${jms.queue.order-status-changed}")
    public void onOrderStatusChanged(OrderStatusChangedEvent event) {
        log.info("Received order status change event: orderId={}, status={}, timestamp={}",
                event.id(),
                event.status(),
                event.timestamp());

        // Here you can add business logic to handle the status change
        // For example:
        // - Update user notifications
        // - Send email/SMS notifications
        // - Update user's order history cache
        // - Trigger other business processes

        log.info("Order status change event processed: orderId={}", event.id());
    }
}
