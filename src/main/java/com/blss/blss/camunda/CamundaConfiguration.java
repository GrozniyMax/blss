package com.blss.blss.camunda;

import org.camunda.bpm.client.ExternalTaskClient;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
@EnableConfigurationProperties(CamundaProperties.class)
public class CamundaConfiguration {

    @Bean
    public ExternalTaskClient externalTaskClient(CamundaProperties properties) {
        return ExternalTaskClient.create()
                .baseUrl(properties.restUrl())
                .workerId(properties.workerId())
                .lockDuration(properties.lockDuration())
                .asyncResponseTimeout(10_000)
                .build();
    }

    @Bean
    public RestClient camundaRestClient(CamundaProperties properties) {
        return RestClient.builder()
                .baseUrl(properties.restUrl())
                .build();
    }
}
