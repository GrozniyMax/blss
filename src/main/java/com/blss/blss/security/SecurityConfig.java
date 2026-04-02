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
 * Role-based access control (per {@link Role} Javadoc):
 * <ul>
 *     <li><b>ADMIN</b> — полный доступ ко всем функциям системы</li>
 *     <li><b>MANAGER</b> — управление заказами (просмотр, редактирование, отмена),
 *         складскими операциями, создание ПВЗ</li>
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

                // Order endpoints
                // POST: USER (own orders), MANAGER, ADMIN
                // GET: USER (own), CONSULTANT (view by ID), MANAGER, ADMIN
                // PATCH: CONSULTANT (update status), MANAGER, ADMIN
                .requestMatchers(HttpMethod.POST, "/order/**").hasAnyRole("USER", "ADMIN")
                .requestMatchers(HttpMethod.GET, "/order/**").hasAnyRole("USER", "CONSULTANT", "MANAGER", "ADMIN")
                .requestMatchers(HttpMethod.PATCH, "/order/**").hasAnyRole("CONSULTANT", "MANAGER", "ADMIN")

                // Inventory endpoints - MANAGER, ADMIN only
                .requestMatchers(HttpMethod.POST, "/inventory/**").hasAnyRole("MANAGER", "ADMIN")
                .requestMatchers(HttpMethod.PUT, "/inventory/**").hasAnyRole("MANAGER", "ADMIN")
                .requestMatchers(HttpMethod.PATCH, "/inventory/**").hasAnyRole("MANAGER", "ADMIN")
                .requestMatchers(HttpMethod.GET, "/inventory/**").hasAnyRole("MANAGER", "ADMIN")

                // PVZ (Pickup Point) endpoints - WAREHOUSE, ADMIN
                .requestMatchers("/mark-delivered").hasAnyRole("WAREHOUSE", "ADMIN")

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
