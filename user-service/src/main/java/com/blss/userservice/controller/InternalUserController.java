package com.blss.userservice.controller;

import com.blss.userservice.dto.input.UserAuthenticationRequest;
import com.blss.userservice.dto.output.UserValidationResponse;
import com.blss.userservice.security.Role;
import com.blss.userservice.xml.XmlUser;
import com.blss.userservice.xml.XmlUserRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Internal controller for service-to-service communication.
 * Should be secured with internal API key in production.
 */
@RestController
@RequestMapping("/internal")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class InternalUserController {

    XmlUserRepository userRepository;

    /**
     * Validate user by username (for order-service to check if user exists)
     */
    @GetMapping("/users/{username}")
    public ResponseEntity<UserValidationResponse> validateUser(@PathVariable String username) {
        log.info("Validating user: {}", username);
        return userRepository.findByUsername(username)
                .map(user -> {
                    List<String> roleNames = user.getRoles() != null
                            ? user.getRoles().getRole().stream()
                                .map(Role::name)
                                .collect(Collectors.toList())
                            : Collections.emptyList();
                    var response = new UserValidationResponse(
                            user.getUsername(),
                            true,
                            user.isEnabled(),
                            roleNames
                    );
                    return ResponseEntity.ok(response);
                })
                .orElse(ResponseEntity.ok(new UserValidationResponse(username, false, false, List.of())));
    }

    /**
     * Authenticate user with password (for order-service JAAS alternative)
     */
    @PostMapping("/users/authenticate")
    public ResponseEntity<UserValidationResponse> authenticateUser(@RequestBody UserAuthenticationRequest request) {
        log.info("Authenticating user: {}", request.username());
        return userRepository.findByUsername(request.username())
                .filter(user -> user.isEnabled() && user.getPassword().equals(request.password()))
                .map(user -> {
                    List<String> roleNames = user.getRoles() != null
                            ? user.getRoles().getRole().stream()
                                .map(Role::name)
                                .collect(Collectors.toList())
                            : Collections.emptyList();
                    var response = new UserValidationResponse(
                            user.getUsername(),
                            true,
                            true,
                            roleNames
                    );
                    return ResponseEntity.ok(response);
                })
                .orElse(ResponseEntity.ok(new UserValidationResponse(request.username(), false, false, List.of())));
    }
}
