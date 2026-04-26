package com.blss.orderservice.jms;

import com.blss.orderservice.domain.order.Status;
import com.blss.orderservice.dto.OrderStatusChangedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderStatusProducer {

    private final JmsTemplate jmsTemplate;

    @Value("${jms.queue.order-status-changed}")
    private String queueName;

    public void sendStatusChange(UUID orderId, Status status) {
        var event = new OrderStatusChangedEvent(orderId, Instant.now(), status.name());
        log.info("Sending order status event to {}: orderId={}, status={}", queueName, orderId, status);
        jmsTemplate.convertAndSend(queueName, event);
    }
}
