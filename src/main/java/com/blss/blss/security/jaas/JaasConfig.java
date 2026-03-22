package com.blss.blss.security.jaas;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.core.annotation.Order;

@Configuration
@RequiredArgsConstructor
@DependsOn("jaasBridge")
@Order(Integer.MIN_VALUE + 100)
public class JaasConfig {
}
