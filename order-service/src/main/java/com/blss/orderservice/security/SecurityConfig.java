package com.blss.orderservice.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Spring Security configuration for order-service with REST-based authentication.
 *
 * Role-based access control:
 * <ul>
 *     <li><b>ADMIN</b> — полный доступ ко всем функциям системы</li>
 *     <li><b>MANAGER</b> — управление заказами, складскими операциями, создание ПВЗ</li>
 *     <li><b>CONSULTANT</b> — выдача заказов: просмотр по ID, обновление статуса</li>
 *     <li><b>WAREHOUSE</b> — только отметка доставки ({@code /mark-delivered})</li>
 *     <li><b>USER</b> — создание заказов, просмотр только своих заказов</li>
 * </ul>
 */
@Slf4j
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true, jsr250Enabled = true)
@RequiredArgsConstructor
public class SecurityConfig {

    private final RestAuthenticationProvider restAuthenticationProvider;
    private final CustomAuthenticationEntryPoint authenticationEntryPoint;
    private final CustomAccessDeniedHandler accessDeniedHandler;

    /**
     * Authentication manager using REST provider.
     */
    @Bean
    public AuthenticationManager authenticationManager() {
        return new ProviderManager(restAuthenticationProvider);
    }

    /**
     * Security filter chain configuration with HTTP Basic authentication.
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        log.info("Configuring Spring Security with REST authentication for order-service");

        http
            .csrf(AbstractHttpConfigurer::disable)
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/actuator/health").permitAll()
                .requestMatchers(org.springframework.http.HttpMethod.POST, "/order/*/bitrix-document")
                    .hasAnyRole("CONSULTANT", "MANAGER", "ADMIN")
                
                // Order endpoints
                .requestMatchers(org.springframework.http.HttpMethod.POST, "/order/**").hasAnyRole("USER", "MANAGER", "ADMIN")
                .requestMatchers(org.springframework.http.HttpMethod.GET, "/order/**").hasAnyRole("USER", "CONSULTANT", "MANAGER", "ADMIN")
                .requestMatchers(org.springframework.http.HttpMethod.PATCH, "/order/**").hasAnyRole("CONSULTANT", "MANAGER", "ADMIN")
                
                // Inventory endpoints - MANAGER, ADMIN only
                .requestMatchers(org.springframework.http.HttpMethod.POST, "/inventory/**").hasAnyRole("MANAGER", "ADMIN")
                .requestMatchers(org.springframework.http.HttpMethod.PUT, "/inventory/**").hasAnyRole("MANAGER", "ADMIN")
                .requestMatchers(org.springframework.http.HttpMethod.PATCH, "/inventory/**").hasAnyRole("MANAGER", "ADMIN")
                .requestMatchers(org.springframework.http.HttpMethod.GET, "/inventory/**").hasAnyRole("MANAGER", "ADMIN")
                
                // PVZ (Pickup Point) endpoints - WAREHOUSE, ADMIN
                .requestMatchers("/mark-delivered").hasAnyRole("WAREHOUSE", "ADMIN")
                
                // Delivery Points - MANAGER, ADMIN
                .requestMatchers("/delivery-points/**").hasAnyRole("MANAGER", "ADMIN")
                
                // All other requests require authentication
                .anyRequest().authenticated()
            )
            .httpBasic(basic -> basic.realmName("Order Service API"))
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint(authenticationEntryPoint)
                .accessDeniedHandler(accessDeniedHandler)
            );

        return http.build();
    }
}
