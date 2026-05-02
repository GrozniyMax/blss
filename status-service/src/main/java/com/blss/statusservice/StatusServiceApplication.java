package com.blss.statusservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
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
public class StatusServiceApplication extends SpringBootServletInitializer {

    public static void main(String[] args) {
        SpringApplication.run(StatusServiceApplication.class, args);
    }

    @Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder application) {
        return application.sources(StatusServiceApplication.class);
    }
}
