package com.blss.blss.security;

import com.blss.blss.security.jaas.BlssJaasConfiguration;
import com.blss.blss.security.jaas.RolePrincipalAuthorityGranter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.jaas.DefaultJaasAuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Spring Security configuration with JAAS integration.
 *
 * Role-based access control:
 * - ADMIN: Full access to all endpoints
 * - MANAGER: Access to orders, inventory, and user management
 * - CONSULTANT: Access to order creation, status updates, and customer operations
 * - WAREHOUSE: Access to inventory management and delivery operations
 * - USER: Access to create orders and view own orders
 */
@Slf4j
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true, jsr250Enabled = true)
@RequiredArgsConstructor
public class SecurityConfig {

    private final CustomAuthenticationEntryPoint authenticationEntryPoint;
    private final CustomAccessDeniedHandler accessDeniedHandler;

    /**
     * JAAS configuration bean.
     */
    @Bean
    public BlssJaasConfiguration blssJaasConfiguration() {
        return new BlssJaasConfiguration();
    }

    /**
     * JAAS authentication provider using DefaultJaasAuthenticationProvider.
     */
    @Bean
    public DefaultJaasAuthenticationProvider jaasAuthenticationProvider(BlssJaasConfiguration jaasConfiguration) {
        DefaultJaasAuthenticationProvider provider = new DefaultJaasAuthenticationProvider();
        provider.setConfiguration(jaasConfiguration);
        provider.setLoginContextName(BlssJaasConfiguration.LOGIN_CONTEXT_NAME);
        provider.setAuthorityGranters(new RolePrincipalAuthorityGranter[] { new RolePrincipalAuthorityGranter() });
        return provider;
    }

    /**
     * Authentication manager using JAAS provider.
     */
    @Bean
    public AuthenticationManager authenticationManager() {
        return new ProviderManager(jaasAuthenticationProvider(blssJaasConfiguration()));
    }

    /**
     * Security filter chain configuration with HTTP Basic authentication.
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        log.info("Configuring Spring Security with JAAS authentication");
        
        http
            // Disable CSRF for stateless REST API
            .csrf(AbstractHttpConfigurer::disable)
            
            // Configure authorization rules
            .authorizeHttpRequests(auth -> auth
                // Public endpoints (if any)
                .requestMatchers("/actuator/health").permitAll()

                // Order endpoints - USER, CONSULTANT, MANAGER, ADMIN
                .requestMatchers(HttpMethod.POST, "/order/**").hasAnyRole("USER", "CONSULTANT", "MANAGER", "ADMIN")
                .requestMatchers(HttpMethod.GET, "/order/**").hasAnyRole("USER", "CONSULTANT", "MANAGER", "ADMIN")
                .requestMatchers(HttpMethod.PATCH, "/order/**").hasAnyRole("CONSULTANT", "MANAGER", "ADMIN")
                
                // Inventory endpoints - WAREHOUSE, MANAGER, ADMIN
                .requestMatchers(HttpMethod.POST, "/inventory/**").hasAnyRole("WAREHOUSE", "MANAGER", "ADMIN")
                .requestMatchers(HttpMethod.PUT, "/inventory/**").hasAnyRole("WAREHOUSE", "MANAGER", "ADMIN")
                .requestMatchers(HttpMethod.PATCH, "/inventory/**").hasAnyRole("WAREHOUSE", "MANAGER", "ADMIN")
                .requestMatchers(HttpMethod.GET, "/inventory/**").hasAnyRole("WAREHOUSE", "MANAGER", "ADMIN", "CONSULTANT")
                
                // PVZ (Pickup Point) endpoints - WAREHOUSE, CONSULTANT, MANAGER, ADMIN
                .requestMatchers("/mark-delivered").hasAnyRole("WAREHOUSE", "CONSULTANT", "MANAGER", "ADMIN")
                
                // User endpoints - ADMIN only
                .requestMatchers("/users/**").hasRole("ADMIN")
                
                // Delivery Points - MANAGER, ADMIN
                .requestMatchers("/delivery-points/**").hasAnyRole("MANAGER", "ADMIN")
                
                // All other requests require authentication
                .anyRequest().authenticated()
            )
            
            // HTTP Basic Authentication
            .httpBasic(basic -> basic
                .realmName("BLSS API")
            )
            
            // Stateless session management
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            
            // Exception handling
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint(authenticationEntryPoint)
                .accessDeniedHandler(accessDeniedHandler)
            );

        return http.build();
    }
}
