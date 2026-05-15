package com.blss.statusservice.jms;

import com.blss.statusservice.dto.OrderStatusChangedEvent;
import com.blss.statusservice.dto.OrderStatusRevertEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;

/**
 * Отправляет запрос на откат статуса в order-service.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OrderStatusRevertProducer {

    private final JmsTemplate jmsTemplate;

    @Value("${jms.queue.order-status-revert}")
    private String revertQueueName;

    public void sendRevertRequest(OrderStatusChangedEvent event, String errorMessage) {
        log.warn("Sending revert request: orderId={}, status={}, error={}",
                event.id(), event.status(), errorMessage);

        var revertEvent = new OrderStatusRevertEvent(
                event,
                errorMessage,
                Instant.now()
        );

        jmsTemplate.convertAndSend(revertQueueName, revertEvent);

        log.info("Request sent: orderId={}", event.id());
    }
}
