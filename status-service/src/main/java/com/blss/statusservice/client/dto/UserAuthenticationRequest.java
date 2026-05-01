package com.blss.statusservice.client.dto;

public record UserAuthenticationRequest(
        String username,
        String password
) {
}
