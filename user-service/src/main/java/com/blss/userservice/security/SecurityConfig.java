package com.blss.userservice.security;

import com.blss.userservice.jaas.BlssJaasConfiguration;
import com.blss.userservice.jaas.RolePrincipalAuthorityGranter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.jaas.DefaultJaasAuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Spring Security configuration with JAAS integration for user-service.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true, jsr250Enabled = true)
@RequiredArgsConstructor
public class SecurityConfig {

    private static final Logger log = LoggerFactory.getLogger(SecurityConfig.class);

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
    public DefaultJaasAuthenticationProvider jaasAuthenticationProvider(BlssJaasConfiguration blssJaasConfiguration) {
        DefaultJaasAuthenticationProvider provider = new DefaultJaasAuthenticationProvider();
        provider.setConfiguration(blssJaasConfiguration);
        provider.setLoginContextName(BlssJaasConfiguration.LOGIN_CONTEXT_NAME);
        provider.setAuthorityGranters(new RolePrincipalAuthorityGranter[] { new RolePrincipalAuthorityGranter() });
        return provider;
    }

    /**
     * Authentication manager using JAAS provider.
     */
    @Bean
    public AuthenticationManager authenticationManager(DefaultJaasAuthenticationProvider jaasAuthenticationProvider) {
        return new ProviderManager(jaasAuthenticationProvider);
    }

    /**
     * Security filter chain configuration with HTTP Basic authentication.
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, AuthenticationManager authenticationManager) throws Exception {
        log.info("Configuring Spring Security with JAAS authentication for user-service");

        http
            .csrf(AbstractHttpConfigurer::disable)
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/actuator/health").permitAll()
                .requestMatchers("/auth/register").permitAll()
                // Internal service-to-service endpoints
                .requestMatchers("/internal/**").permitAll()
                .requestMatchers("/users/**").hasAnyRole("ADMIN", "MANAGER")
                .anyRequest().authenticated()
            )
            .authenticationManager(authenticationManager)
            .httpBasic(basic -> basic.realmName("User Service API"))
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint(authenticationEntryPoint)
                .accessDeniedHandler(accessDeniedHandler)
            );

        return http.build();
    }
}
