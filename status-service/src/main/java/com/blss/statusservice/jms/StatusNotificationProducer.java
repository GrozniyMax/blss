package com.blss.statusservice.jms;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Component;

/**
 * JMS producer for sending status-related events.
 */
@Component
@RequiredArgsConstructor
public class StatusNotificationProducer {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(StatusNotificationProducer.class);

    private final JmsTemplate jmsTemplate;

    @Value("${jms.queue.notifications}")
    private String queueName;

    /**
     * Sends notification event to JMS queue.
     *
     * @param message Message to send
     */
    public void sendNotification(String message) {
        log.info("Sending notification to queue {}: {}", queueName, message);
        jmsTemplate.convertAndSend(queueName, message);
        log.info("Notification sent successfully");
    }
}
