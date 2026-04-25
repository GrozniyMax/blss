package com.blss.statusservice.client;

import com.blss.statusservice.client.dto.UserAuthenticationRequest;
import com.blss.statusservice.client.dto.UserValidationResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;

/**
 * Client for calling user-service REST API.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class UserServiceClient {

    private final WebClient.Builder webClientBuilder;

    @Value("${user.service.url:http://localhost:25104}")
    private String userServiceUrl;

    /**
     * Validate user by username.
     * @return Mono with UserValidationResponse
     */
    public Mono<UserValidationResponse> validateUser(String username) {
        log.debug("Validating user {} via user-service", username);
        return webClientBuilder.build()
                .get()
                .uri("{baseUrl}/internal/users/{username}", 
                        userServiceUrl, username)
                .retrieve()
                .bodyToMono(UserValidationResponse.class)
                .timeout(Duration.ofSeconds(5))
                .onErrorReturn(new UserValidationResponse(username, false, false, java.util.List.of()));
    }

    /**
     * Authenticate user with password.
     * @return Mono with UserValidationResponse
     */
    public Mono<UserValidationResponse> authenticateUser(String username, String password) {
        log.debug("Authenticating user {} via user-service", username);
        var request = new UserAuthenticationRequest(username, password);
        return webClientBuilder.build()
                .post()
                .uri("{baseUrl}/internal/users/authenticate", userServiceUrl)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(UserValidationResponse.class)
                .timeout(Duration.ofSeconds(5))
                .onErrorReturn(new UserValidationResponse(username, false, false, java.util.List.of()));
    }

    /**
     * Check if user exists and is enabled.
     * @return true if user exists and is enabled
     */
    public boolean existsByUsername(String username) {
        return validateUser(username)
                .map(response -> response.exists() && response.enabled())
                .block(Duration.ofSeconds(5));
    }
}
