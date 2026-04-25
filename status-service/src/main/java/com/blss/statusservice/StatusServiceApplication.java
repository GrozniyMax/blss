package com.blss.statusservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.jms.annotation.EnableJms;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Status Service Application.
 * 
 * Responsible for:
 * - Tracking order status changes
 * - Sending notifications to users
 * - Status history and analytics
 */
@SpringBootApplication
@EnableJms
@EnableAsync
public class StatusServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(StatusServiceApplication.class, args);
    }
}
