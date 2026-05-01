package com.blss.statusservice.security;

import com.blss.statusservice.client.UserServiceClient;
import com.blss.statusservice.client.dto.UserValidationResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Authentication provider that validates credentials against user-service via REST API.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RestAuthenticationProvider implements AuthenticationProvider {

    private final UserServiceClient userServiceClient;

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        String username = authentication.getName();
        String password = (String) authentication.getCredentials();

        log.debug("Authenticating user {} via REST API", username);

        try {
            UserValidationResponse response = userServiceClient.authenticateUser(username, password).block();

            log.info("Received response from user-service: {}", response);

            if (response == null || !response.exists() || !response.enabled()) {
                log.debug("Authentication failed for user {}: user not found or disabled", username);
                throw new BadCredentialsException("Invalid username or password");
            }

            List<SimpleGrantedAuthority> authorities = response.roles().stream()
                    .map(role -> new SimpleGrantedAuthority("ROLE_" + role.name()))
                    .collect(Collectors.toList());

            log.debug("User {} authenticated successfully with roles: {}", username, authorities);

            return new UsernamePasswordAuthenticationToken(username, password, authorities);

        } catch (Exception e) {
            log.error("Authentication failed for user {}: {}", username, e.getMessage());
            throw new BadCredentialsException("Authentication failed: " + e.getMessage());
        }
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication);
    }
}
