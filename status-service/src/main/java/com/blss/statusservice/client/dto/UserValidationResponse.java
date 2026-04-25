package com.blss.statusservice.client.dto;

import java.util.List;

/**
 * Response from user-service for user validation.
 */
public record UserValidationResponse(
        String username,
        boolean exists,
        boolean enabled,
        List<String> roles
) {
}
