package com.blss.statusservice.client.dto;

/**
 * Request for user authentication.
 */
public record UserAuthenticationRequest(
        String username,
        String password
) {
}
