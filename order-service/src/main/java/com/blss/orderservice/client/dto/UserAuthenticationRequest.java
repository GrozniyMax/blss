package com.blss.orderservice.client.dto;

public record UserAuthenticationRequest(
        String username,
        String password
) {
}
