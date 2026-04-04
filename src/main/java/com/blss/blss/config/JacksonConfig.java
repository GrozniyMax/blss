package com.blss.blss.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

/**
 * Jackson configuration for Java 8 date/time support.
 */
@Configuration
public class JacksonConfig {

    @Bean
    public Jackson2ObjectMapperBuilder jacksonBuilder() {
        return Jackson2ObjectMapperBuilder.json()
                .modulesToInstall(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
    }
}
