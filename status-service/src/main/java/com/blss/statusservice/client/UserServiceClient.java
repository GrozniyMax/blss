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
        log.info("Validating user {} via user-service", username);
        return webClientBuilder
                .baseUrl(userServiceUrl)
                .build()
                .get()
                .uri(uriBuilder -> uriBuilder
                        .path("/internal/users/{username}")
                        .build(username))
                .retrieve()
                .bodyToMono(UserValidationResponse.class)
                .timeout(Duration.ofSeconds(5))
                .doOnSuccess(response -> log.info("User {} validation result: exists={}, enabled={}, roles={}", 
                        username, response.exists(), response.enabled(), response.roles()))
                .onErrorResume(e -> {
                    log.error("Error validating user {}: {}", username, e.getMessage(), e);
                    return Mono.just(new UserValidationResponse(username, false, false, java.util.List.of()));
                });
    }

    /**
     * Authenticate user with password.
     * @return Mono with UserValidationResponse
     */
    public Mono<UserValidationResponse> authenticateUser(String username, String password) {
        log.info("Authenticating user {} via user-service", username);
        var request = new UserAuthenticationRequest(username, password);
        return webClientBuilder
                .baseUrl(userServiceUrl)
                .build()
                .post()
                .uri(uriBuilder -> uriBuilder
                        .path("/internal/users/authenticate")
                        .build())
                .bodyValue(request)
                .retrieve()
                .bodyToMono(UserValidationResponse.class)
                .timeout(Duration.ofSeconds(5))
                .doOnSuccess(response -> log.info("User {} authentication result: exists={}, enabled={}, roles={}", 
                        username, response.exists(), response.enabled(), response.roles()))
                .onErrorResume(e -> {
                    log.error("Error authenticating user {}: {}", username, e.getMessage(), e);
                    return Mono.just(new UserValidationResponse(username, false, false, java.util.List.of()));
                });
    }

    /**
     * Check if user exists.
     * @return true if user exists and is enabled
     */
    public boolean existsByUsername(String username) {
        log.info("Checking if user {} exists", username);
        boolean exists = validateUser(username)
                .map(response -> response.exists() && response.enabled())
                .block(Duration.ofSeconds(5));
        log.info("User {} exists check result: {}", username, exists);
        return exists;
    }
}
