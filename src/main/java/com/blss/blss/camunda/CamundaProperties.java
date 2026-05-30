package com.blss.blss.camunda;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "camunda")
public record CamundaProperties(
        String restUrl,
        String workerId,
        Long lockDuration,
        Processes processes
) {
    public record Processes(
            String productCreationKey,
            String orderCreationKey,
            String orderPickupKey
    ) {
    }
}
