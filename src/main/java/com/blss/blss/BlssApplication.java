package com.blss.blss;

import org.camunda.bpm.client.spring.annotation.EnableExternalTaskClient;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableAsync
@EnableScheduling
@EnableExternalTaskClient
@SpringBootApplication
public class BlssApplication {

    public static void main(String[] args) {
        SpringApplication.run(BlssApplication.class, args);
    }
}
