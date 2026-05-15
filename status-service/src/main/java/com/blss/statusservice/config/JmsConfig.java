package com.blss.statusservice.config;

import com.blss.statusservice.dto.OrderStatusChangedEvent;
import com.blss.statusservice.dto.OrderStatusRevertEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jms.support.converter.MappingJackson2MessageConverter;
import org.springframework.jms.support.converter.MessageType;

import java.util.Map;

@Configuration
public class JmsConfig {

    @Bean
    public MappingJackson2MessageConverter jacksonJmsMessageConverter(ObjectMapper objectMapper) {
        var converter = new MappingJackson2MessageConverter();
        converter.setTargetType(MessageType.TEXT);
        converter.setTypeIdPropertyName("_type");
        converter.setObjectMapper(objectMapper);
        converter.setTypeIdMappings(Map.of(
                "orderStatusChangedEvent", OrderStatusChangedEvent.class,
                "orderStatusRevertEvent", OrderStatusRevertEvent.class
        ));
        return converter;
    }
}
