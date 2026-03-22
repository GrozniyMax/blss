package com.blss.blss.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class JacksonConfig {

    @Bean
    @Primary // Mark as primary if other potential ObjectMapper beans might exist
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        // You can add any custom configurations here, e.g.:
        // mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        // mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return mapper;
    }
}
